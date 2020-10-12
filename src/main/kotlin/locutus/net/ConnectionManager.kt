package locutus.net

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.*
import locutus.tools.crypto.*
import locutus.tools.crypto.rsa.*
import mu.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread


/**
 * Responsible for securely transmitting [Message]s between [Peer]s.
 */
class ConnectionManager(
    val port: Int,
    val myKey: RSAKeyPair,
    private val open: Boolean
) {

    companion object {
        val protoBuf = ProtoBuf { encodeDefaults = false }
        val pingEvery: Duration = Duration.ofSeconds(30)
        val dropConnectionAfter: Duration = pingEvery.multipliedBy(10)
        val keepAliveExtractor = Extractor<Message.Keepalive, Unit>("keepAlive") {Unit}
    }

    private val logger = KotlinLogging.logger {}

    @PublishedApi
    internal val scope = MainScope()

    private val connections = ConcurrentHashMap<Peer, Connection>()

    private val removeConnectionListeners = ConcurrentLinkedQueue<(Peer, String) -> Unit>()

    @PublishedApi
    internal val router = MessageRouter()

    private val channel: DatagramChannel = DatagramChannel.open()

    init {
        withLoggingContext("port" to port.toString()) {
            logger.info { "Listening on UDP port $port" }
            startListenThread()
            launchKeepaliveCoroutine()
            listenForKeepalives()
        }
    }

    private fun listenForKeepalives() {
        listen(keepAliveExtractor, Unit, NEVER) {
            val connection = connections[sender]
            if (connection != null) {
                connection.lastKeepaliveReceived = Instant.now()
            }
        }
    }

    private fun launchKeepaliveCoroutine() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                for ((peer, connection) in connections) {
                    delay(pingEvery.dividedBy(connections.size.toLong()))
                    if (connection.lastKeepaliveReceived != null && Duration.between(
                            connection.lastKeepaliveReceived,
                            Instant.now()
                        ) > dropConnectionAfter
                    ) {
                        removeConnection(peer, "Time since last keepalive exceeded $dropConnectionAfter")
                    } else {
                        send(peer, Message.Keepalive())
                    }
                }
            }
        }
    }

    private fun startListenThread() {
        channel.socket().bind(InetSocketAddress(port))
        val buf = ByteBuffer.allocateDirect(Constants.MAX_UDP_PACKET_SIZE + 200)
        thread {
            while (true) {
                val sender = Peer(channel.receive(buf) as InetSocketAddress)
                logger.debug { "Packet received from $sender of length ${buf.remaining()}" }
                buf.flip()
                val byteArray = ByteArray(buf.remaining())
                buf.get(byteArray)
                buf.clear()
                scope.launch(Dispatchers.IO) {
                    handleReceivedPacket(sender, byteArray)
                }
            }
        }
    }

    fun addConnection(
        peerKey: PeerKey,
        unsolicited: Boolean
    ): Connection {
        val (peer, pubKey) = peerKey
        require(!connections.containsKey(peer)) { "Connection to $peer already exists" }

        withLoggingContext("peer" to peer.toString()) {
            logger.info { "Adding ${if (unsolicited) "outbound" else "symmetric"} connection to $peer" }
            val outboundKey = AESKey.generate()
            val encryptedOutboundKey = pubKey.encrypt(outboundKey.bytes).ciphertext
            val type = when (unsolicited) {
                true -> {
                    Connection.Type.Outbound(pubKey, false, outboundKey, encryptedOutboundKey)
                }
                false -> {
                    Connection.Type.Symmetric(pubKey, false, outboundKey, encryptedOutboundKey, null)
                }
            }
            val connection = Connection(peer, type, null)
            connections[peer] = connection
            return connection
        }
    }

    fun onRemoveConnection(block: (Peer, String) -> Unit) {
        removeConnectionListeners += block
    }

    fun removeConnection(peer: Peer, reason: String) {
        send(peer, Message.Ring.CloseConnection(reason))
        connections.remove(peer)
        for (listener in removeConnectionListeners) {
            listener.invoke(peer, reason)
        }
    }

    fun send(to: Peer, message: Message) {
        logger.debug { "Sending $message to $to" }
        val connection = connections[to]
        requireNotNull(connection)
        val serializedMessage = protoBuf.encodeToByteArray(Message.serializer(), message)
        connection.type.let { connectionType ->
            val symKey: AESKey = when (connectionType) {
                is Connection.Type.Symmetric -> connectionType.outboundKey
                is Connection.Type.Outbound -> connectionType.outboundKey
                is Connection.Type.Inbound -> connectionType.inboundKey.aesKey
            }

            val encryptedMessage = symKey.encrypt(serializedMessage)
            val keyPrepend: List<ByteArray> = when (val type = connection.type) {
                is Connection.Type.Symmetric -> if (type.outboundKeyReceived) {
                    emptyList()
                } else {
                    listOf(type.encryptedOutboundKeyPrefix)
                }
                is Connection.Type.Outbound -> if (type.outboundKeyReceived) {
                    emptyList()
                } else {
                    listOf(type.encryptedOutboundKeyPrefix)
                }
                is Connection.Type.Inbound -> emptyList()
            }

            val outboundRaw = (keyPrepend + encryptedMessage).merge()
            logger.debug { "Sending ${outboundRaw.size}b message to $to" }
            channel.send(ByteBuffer.wrap(outboundRaw), to.asSocketAddress)
        }
    }

    /**
     * Send a message and listen for a response, this ensures that the response listener
     * is registered before the message is sent to avoid possible race condition.
     */
    inline fun <reified MType : Message, KeyType : Any> sendReceive(
            to: Peer,
            message: Message,
            extractor: Extractor<MType, KeyType>,
            key: KeyType,
            timeout: Duration?,
            noinline block: (MessageReceiver<MType>).() -> Unit
    ) {
        router.listen(extractor, key, timeout, block)
        send(to, message)
    }

    /**
     * Send a message and listen for a response, this ensures that the response listener
     * is registered before the message is sent to avoid possible race condition.
     *
     * Will resend the message every [retryDelay] up to [retries] times until a resposne is received.
     * This shouldn't be a problem as [ConnectionManager] will automatically disregard duplicate messages (determined
     * by their [MessageId].
     */
    inline fun <reified MType : Message, KeyType : Any> sendReceive(
            to: Peer,
            message: Message,
            extractor: Extractor<MType, KeyType>,
            key: KeyType,
            retries: Int,
            retryDelay: Duration,
            noinline block: (MessageReceiver<MType>).() -> Unit
    ) {
        val responseReceived = AtomicBoolean(false)
        sendReceive(to, message, extractor, key, retryDelay.multipliedBy(retries.toLong() + 1)) {
            val xSender = sender
            val xMessage: MType = message as MType // Not sure why this cast is necessary
            responseReceived.set(true)
            block(object : MessageReceiver<MType> {
                override val sender: Peer = xSender
                override val message: MType = xMessage
            })
        }
        scope.launch(Dispatchers.IO) {
            for (retryNo in 1..retries) {
                delay(retryDelay)
                if (responseReceived.get()) break
                send(to, message)
            }
        }
    }

    /**
     * Listen for incoming messages, see [MessageRouter.listen]
     */
    inline fun <reified MType : Message, KeyType : Any> listen(
            for_: Extractor<MType, KeyType>,
            key: KeyType,
            timeout: Duration?,
            noinline block: (MessageReceiver<MType>).() -> Unit
    ) {
        router.listen(for_, key, timeout, block)
    }

    // TODO: This should be an expiring cache
    private val receivedMessageIds = ConcurrentSkipListSet<MessageId>()

    private fun handleReceivedPacket(sender: Peer, rawPacket: ByteArray) {
        withLoggingContext("sender" to sender.toString()) {
            logger.debug { "packetReceived($sender, ${rawPacket.size} bytes)" }

            var connection: Connection? = connections[sender]

            val knownSymKeyPrefix: ByteArray? = if (connection != null) when (val type = connection.type) {
                is Connection.Type.Symmetric -> type.inboundKey?.encryptedPrefix
                is Connection.Type.Outbound -> null
                is Connection.Type.Inbound -> type.inboundKey.encryptedPrefix
            } else null

            val decryptedPayload: ByteArray = when {

                connection == null -> {
                    logger.debug { "Packet received from unknown sender" }
                    if (!open) {
                        logger.warn("Disregarding packet from unknown sender because I'm not open")
                        return
                    }
                    logger.debug { "Packet received from unknown sender, assume its prepended, extract and use" }
                    val splitPacket = rawPacket.splitPacket()
                    val symKey = AESKey(myKey.private.decrypt(RSAEncrypted(splitPacket.encryptedAESKey)))
                    val inboundType = Connection.Type.Inbound(InboundKey(symKey, splitPacket.encryptedAESKey))
                    connection = Connection(sender, inboundType, null)
                    connections[sender] = connection
                    symKey.decrypt(splitPacket.payload)
                }

                knownSymKeyPrefix != null && rawPacket.startsWith(knownSymKeyPrefix) -> {
                    logger.debug { "Packet has prepended symkey, but we've already received it" }
                    connection.type.decryptKey?.decrypt(rawPacket.splitPacket().payload)
                        ?: error("knownSymKeyPrefix != null but connection.type.decryptKey is null, this shouldn't happen")
                }

                connection.type.decryptKey == null -> {
                    logger.debug { "Packet received, decrypt key unknown, assume its prepended and parse it" }
                    val splitPacket = rawPacket.splitPacket()
                    val symKey = AESKey(myKey.private.decrypt(RSAEncrypted(splitPacket.encryptedAESKey)))
                    when (val type = connection.type) {
                        is Connection.Type.Symmetric -> {
                            type.inboundKey = InboundKey(symKey, splitPacket.encryptedAESKey)
                            symKey.decrypt(splitPacket.payload)
                        }
                        else -> error("Packet received, decrypt key unknown but connection is known, but connection type isn't Symmetric, it is ${connection.type::class.simpleName}")
                    }
                }

                connection.type.decryptKey != null -> {
                    logger.debug { "Packet received, decrypt key is known and isn't prepended, assume entire packet is payload" }
                    connection.type.decryptKey?.decrypt(rawPacket)
                        ?: error("connection.type.decryptKey shouldn't be null")
                }

                else -> error("Unhandled condition while decrypting payload")
            }

            val message = protoBuf.decodeFromByteArray(Message.serializer(), decryptedPayload)
            handleMessage(connection, message)

        }
    }

    private fun handleMessage(connection: Connection, message: Message) {
        withLoggingContext("sender" to connection.peer.toString(), "message" to message::class.toString()) {
            if (message.id in receivedMessageIds) {
                logger.warn { "Disregarding message ${message.id} because it has already been received" }
            } else {
                logger.debug { "Handling message: ${message::class.simpleName}" }
                if (message !is Initiate || !message.isInitiate) {
                    logger.debug { "Message is response, indicating outboundKey has been received" }
                    when (val type = connection.type) {
                        is Connection.Type.Symmetric -> type.outboundKeyReceived = true
                        is Connection.Type.Outbound -> type.outboundKeyReceived = true
                    }
                } else {
                    logger.debug { "Message is not response" }
                }
                router.route(connection.peer, message)
            }
        }
    }


}

private class SplitPacket(val encryptedAESKey: ByteArray, val payload: ByteArray)

private fun ByteArray.splitPacket(): SplitPacket {
    return SplitPacket(
        this.copyOf(AESKey.RSA_ENCRYPTED_SIZE),
        this.copyOfRange(AESKey.RSA_ENCRYPTED_SIZE, this.size)
    )
}
