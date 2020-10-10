package locutus.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.*
import locutus.net.messages.MessageRouter.Extractor
import locutus.net.messages.MessageRouter.SenderMessage
import locutus.tools.crypto.AESKey
import locutus.tools.crypto.merge
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.crypto.rsa.encrypt
import locutus.tools.crypto.startsWith
import mu.KotlinLogging
import mu.withLoggingContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
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
        val pingEvery : Duration = Duration.ofSeconds(30)
        val dropConnectionAfter : Duration = pingEvery.multipliedBy(10)
        val keepAliveExtractor = object : Extractor<Message.Keepalive, Unit>("keepAlive") {
            override fun invoke(p1: SenderMessage<Message.Keepalive>) = Unit
        }
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
                logger.trace { "Packet received from $sender of length ${buf.remaining()}" }
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
        peerKey: PeerKey
    ) : Connection {
        val (peer, pubKey) = peerKey
        require(!connections.containsKey(peer)) { "Connection to $peer already exists" }

        withLoggingContext("peer" to peer.toString()) {
            logger.info { "Establishing connection to $peer" }
            val outboundKey = AESKey.generate()
            val encryptedOutboundKey = pubKey.encrypt(outboundKey.bytes).ciphertext
            val connection = Connection(
                peer = peer,
                pubKey = pubKey,
                outboundKeyReceived = false,
                outboundKey = outboundKey,
                encryptedOutboundKeyPrefix = encryptedOutboundKey,
                inboundKey = null,
                lastKeepaliveReceived = null
            )
            connections[peer] = connection
            return connection
        }
    }

    fun onRemoveConnection(block : (Peer, String) -> Unit) {
        removeConnectionListeners += block
    }

    fun removeConnection(peer : Peer, reason : String) {
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
        val encryptedMessage = connection.outboundKey.encrypt(serializedMessage)
        val keyPrepend: List<ByteArray> =
            if (connection.outboundKeyReceived) {
                emptyList()
            } else {
                listOf(connection.encryptedOutboundKeyPrefix)
            }
        val outboundRaw = (keyPrepend + encryptedMessage).merge()
        logger.trace { "Sending ${outboundRaw.size}b message to $to" }
        channel.send(ByteBuffer.wrap(outboundRaw), to.asSocketAddress)
    }

    /**
     * Send a message and listen for a response, this ensures that the response listener
     * is registered before the message is sent to avoid possible race condition.
     */
    inline fun <reified MType : Message, KeyType : Any> sendReceive(
        to: Peer,
        message: Message,
        extractor: MessageRouter.Extractor<MType, KeyType>,
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
        extractor: MessageRouter.Extractor<MType, KeyType>,
        key: KeyType,
        timeout: Duration?,
        noinline block: (MessageReceiver<MType>).() -> Unit
    ) {
        router.listen(extractor, key, timeout, block)
    }

    // TODO: This should be an expiring cache
    private val receivedMessageIds = ConcurrentSkipListSet<MessageId>()

    private suspend fun handleReceivedPacket(sender: Peer, rawPacket: ByteArray) {
        withLoggingContext("sender" to sender.toString()) {
            logger.debug { "packetReceived($sender, ${rawPacket.size} bytes)" }
            val connection : Connection = connections[sender]
                ?: if (open) {
                    TODO()
                    //val c = Connection(sender, null, false, AESKey.generate(), )
                    //connections[sender] = c
                   // c
                } else {
                    logger.info { "Disregarding packet from unknown sender $sender" }
                    return
                }

            logger.trace { "$sender is connected" }
            val encryptedPayload = connection.inboundKey.let { inboundKey ->
                if (inboundKey?.inboundKeyPrefix != null && rawPacket.startsWith(inboundKey.inboundKeyPrefix)) {
                    logger.debug { "$sender has prepended AES key although it is already known, disregarding" }
                    rawPacket.splitPacket().payload
                } else {
                    rawPacket
                }
            }
            val inboundKey = connection.inboundKey
                requireNotNull(inboundKey) { "Can't decrypt packet without inboundKey" }
                val decryptedPayload = inboundKey.aesKey.decrypt(encryptedPayload)
                val message = protoBuf.decodeFromByteArray(Message.serializer(), decryptedPayload)
                handleMessage(connection, message)

        }
    }

    private suspend fun handleMessage(connection: Connection, message: Message) {
        withLoggingContext("sender" to connection.peer.toString(), "message" to message::class.toString()) {
            if (message.id in receivedMessageIds) {
                logger.warn { "Disregarding message ${message.id} because it has already been received" }
            } else {
                logger.debug { "Handling message: ${message::class.simpleName}" }
                if (message !is Initiate || !message.hasYourKey) {
                    logger.debug { "Message is response, indicating outboundKey has been received" }
                    if (!connection.outboundKeyReceived) connection.outboundKeyReceived = true
                } else {
                    logger.debug { "Message is not response" }
                }
                router.route(connection.peer, message)
            }
        }
    }


}

private class SplitPacket(val decryptKey: ByteArray, val payload: ByteArray)

private fun ByteArray.splitPacket(): SplitPacket {
    return SplitPacket(this.copyOf(AESKey.RSA_ENCRYPTED_SIZE), this.copyOfRange(AESKey.RSA_ENCRYPTED_SIZE, this.size))
}
