package locutus.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants.MAX_UDP_PACKET_SIZE
import locutus.net.messages.*
import locutus.tools.crypto.AESKey
import locutus.tools.crypto.merge
import locutus.tools.crypto.rsa.RSAEncrypted
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.crypto.rsa.decrypt
import locutus.tools.crypto.rsa.encrypt
import locutus.tools.crypto.startsWith
import mu.KotlinLogging
import mu.withLoggingContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass


/**
 * Responsible for securely transmitting [Message]s between [Peer]s.
 */
class ConnectionManager(
    val myKey: RSAKeyPair,
    val transport: Transport,
) {

    constructor(port: Int, isOpen: Boolean, myKey: RSAKeyPair) :
            this(myKey, UDPTransport(port, isOpen))

    companion object {
        val protoBuf = ProtoBuf { encodeDefaults = false }
        val pingEvery: Duration = Duration.ofSeconds(30)
        val dropConnectionAfter: Duration = pingEvery.multipliedBy(10)
        val keepAliveExtractor = Extractor<Message.Keepalive, Unit>("keepAlive") { Unit }
    }

    private val logger = KotlinLogging.logger {}

    @PublishedApi
    internal val scope = MainScope()

    private val connections = ConcurrentHashMap<Peer, Connection>()

    private val removeConnectionListeners = ConcurrentLinkedQueue<(Peer, String) -> Unit>()

    @PublishedApi
    internal val router = MessageRouter()


    init {
        scope.launch(Dispatchers.IO) {
            for ((sender, packet) in transport.recepient) {
                handleReceivedPacket(sender, packet)
            }
        }
        launchKeepaliveCoroutine()
        listenForKeepalives()
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

    fun addConnection(
        peerKey: PeerKey,
        unsolicited: Boolean
    ): Connection {
        val (peer, pubKey) = peerKey
        logger.info { "Adding connection to $peer" }
        if (connections.containsKey(peer)) {
            logger.debug { "Connection to $peer already exists, won't add again" }
            return connections.getValue(peer)
        } else {
            logger.info { "Adding ${if (unsolicited) "outbound" else "symmetric"} connection to $peer" }
            val outboundKey = AESKey.generate()
            val outboundKeyBytes = outboundKey.bytes
            require(outboundKeyBytes.size == AESKey.KEY_SIZE_BYTES) { "Outbound key size should be ${AESKey.KEY_SIZE_BYTES} bytes, but was ${outboundKeyBytes.size} bytes" }
            val encryptedOutboundKey = pubKey.encrypt(outboundKey.bytes).ciphertext
            require(encryptedOutboundKey.size == AESKey.RSA_ENCRYPTED_SIZE) { "Expected encryptedOutboundKey.size to be ${AESKey.RSA_ENCRYPTED_SIZE} bytes, but was ${encryptedOutboundKey.size} bytes" }
            logger.info { "EncryptedOutboundKey size is ${encryptedOutboundKey.size}" }
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
        requireNotNull(connection) { "Trying to send ${message::class.simpleName} to $to, but it's not connected" }
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
            require(outboundRaw.size <= MAX_UDP_PACKET_SIZE) { "Message size ${outboundRaw.size} exceeds MAX_UDP_PACKET_SIZE ($MAX_UDP_PACKET_SIZE)" }
            logger.debug { "Sending ${outboundRaw.size}b message to $to" }
            transport.send(to, outboundRaw)
        }
    }

    @PublishedApi
    internal val replyExtractorMap = ConcurrentHashMap<KClass<*>, ReplyExtractor<*>>()

    /**
     * Send a message and listen for a response, this ensures that the response listener
     * is registered before the message is sent to avoid possible race condition.
     *
     * Will resend the message every [retryDelay] up to [retries] times until a resposne is received.
     * This shouldn't be a problem as [ConnectionManager] will automatically disregard duplicate messages (determined
     * by their [MessageId].
     */
    inline fun <reified ReplyType : Message> send(
        to: Peer,
        message: Message,
        retries: Int = 5,
        retryDelay: Duration = Duration.ofMillis(200),
        listenFor: Duration = Duration.ofSeconds(60),
        noinline block: (MessageReceiver<ReplyType>).() -> Unit
    ) {
        assert(Reply::class.java.isAssignableFrom(ReplyType::class.java)) { "ReplyType must implement Reply interface" }

        val responseReceived = AtomicBoolean(false)
        val replyExtractor =
            replyExtractorMap.computeIfAbsent(ReplyType::class) { ReplyExtractor<ReplyType>("reply-extractor-${ReplyType::class.qualifiedName}") }
        router.listen(replyExtractor as ReplyExtractor<ReplyType>, PeerId(to, message.id), listenFor, {
            val xSender = sender
            val xMessage: ReplyType = received
            responseReceived.set(true)
            block(object : MessageReceiver<ReplyType> {
                override val sender: Peer = xSender
                override val received: ReplyType = xMessage
            })
        })
        send(to, message)
        scope.launch(Dispatchers.IO) {
            val startTime = Instant.now()
            for (retryNo in 1..retries) {
                delay(retryDelay)
                if (Duration.between(startTime, Instant.now()) > listenFor) {
                    break
                }
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
                    if (!transport.isOpen) {
                        logger.info("Disregarding packet from unknown sender because I'm not open")
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
                    require(knownSymKeyPrefix == null || !rawPacket.startsWith(knownSymKeyPrefix)) { "Packet starts with knownSymKeyPrefix but shouldn't" }
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
        withLoggingContext("sender" to connection.peer.toString(), "message" to message::class.simpleName.toString()) {
            if (message.id in receivedMessageIds) {
                logger.warn { "Disregarding message ${message.id} because it has already been received" }
            } else {
                logger.debug { "Handling message: ${message::class.simpleName}" }
                if (message !is CanInitiate || !message.isInitiate) {
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
