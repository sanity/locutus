package locutus.net

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.*
import locutus.net.messages.MessageRouter.*
import locutus.tools.crypto.*
import locutus.tools.crypto.rsa.*
import mu.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.*
import kotlin.concurrent.thread

/**
 * Responsible for securely transmitting [Message]s between [Peer]s.
 */
@ExperimentalSerializationApi
class ConnectionManager(
    val port: Int,
    val myKey: RSAKeyPair,
    private val open: Boolean
) {

    companion object {
        val protoBuf = ProtoBuf { encodeDefaults = false }
    }

    private val logger = KotlinLogging.logger {}

    private val connections = ConcurrentHashMap<Peer, Connection>()

    @PublishedApi
    internal val router = MessageRouter()

    private val channel: DatagramChannel = DatagramChannel.open()

    init {
        withLoggingContext("port" to port.toString()) {
            channel.socket().bind(InetSocketAddress(port))
            logger.info { "Listening on UDP port $port" }
            val buf = ByteBuffer.allocateDirect(Constants.MAX_UDP_PACKET_SIZE + 200)
            thread {
                while (true) {
                    val sender = Peer(channel.receive(buf) as InetSocketAddress)
                    logger.trace { "Packet received from $sender of length ${buf.remaining()}" }
                    buf.flip()
                    val byteArray = ByteArray(buf.remaining())
                    buf.get(byteArray)
                    buf.clear()
                    GlobalScope.launch(Dispatchers.IO) {
                        handleReceivedPacket(sender, byteArray)
                    }
                }
            }
        }
    }

    /**
     * @param peer The peer to connect to
     * @param isOpen Is [peer] open?
     */
    fun addConnection(
        peerWithKey: PeerWithKey,
        isOpen: Boolean
    ) {
        val (peer, pubKey) = peerWithKey
        require(!connections.containsKey(peer)) { "Connection to $peer already exists" }

        withLoggingContext("peer" to peer.toString(), "isOpen" to isOpen.toString()) {
            logger.info { "Establishing connection to $peer" }
            val outboundKey = AESKey.generate()
            val encryptedOutboundKey = pubKey.encrypt(outboundKey.bytes).ciphertext
            val connection = Connection(
                peer = peer,
                pubKey = pubKey,
                outboundKeyReceived = false,
                outboundKey = outboundKey,
                encryptedOutboundKeyPrefix = encryptedOutboundKey,
                inboundKey = if (isOpen) InboundKey(outboundKey, null) else null,
            )
            connections[peer] = connection
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
        key: KeyType
    ): ReceiveChannel<SenderMessage<MType>> {
        val channel : Channel<SenderMessage<MType>> = router.listeners
            .computeIfAbsent(MType::class) { ConcurrentHashMap<ExtractorLabel, org.eclipse.collections.impl.map.mutable.ConcurrentHashMap<locutus.net.messages.KeyType, SendChannel<SenderMessage<Message>>>>() }
            .computeIfAbsent(extractor.label) { ConcurrentHashMap<locutus.net.messages.KeyType, SendChannel<SenderMessage<Message>>>() }
            .compute(key) { _, existingChannel ->
                if (existingChannel != null) {
                    error("Listener already exists for ${extractor.label}->$key")
                } else {
                    Channel()
                }
            } as Channel<SenderMessage<MType>>
        router.extractors.putIfAbsent(extractor.label, extractor as Extractor<Message, Any>)
        channel.invokeOnClose {
            router.listeners[MType::class]?.get(extractor.label)?.remove(key)
        }
        val channel = channel
        send(to, message)
        return channel
    }

    /**
     * Listen for incoming messages
     */
    inline fun <reified MType : Message, KeyType : Any> listen(
        extractor: MessageRouter.Extractor<MType, KeyType>,
        key: KeyType
    ): ReceiveChannel<SenderMessage<MType>> {
        val channel : Channel<SenderMessage<MType>> = router.listeners
            .computeIfAbsent(MType::class) { ConcurrentHashMap<ExtractorLabel, org.eclipse.collections.impl.map.mutable.ConcurrentHashMap<locutus.net.messages.KeyType, SendChannel<SenderMessage<Message>>>>() }
            .computeIfAbsent(extractor.label) { ConcurrentHashMap<locutus.net.messages.KeyType, SendChannel<SenderMessage<Message>>>() }
            .compute(key) { _, existingChannel ->
                if (existingChannel != null) {
                    error("Listener already exists for ${extractor.label}->$key")
                } else {
                    Channel()
                }
            } as Channel<SenderMessage<MType>>
        router.extractors.putIfAbsent(extractor.label, extractor as Extractor<Message, Any>)
        channel.invokeOnClose {
            router.listeners[MType::class]?.get(extractor.label)?.remove(key)
        }
        return channel
    }

    // TODO: This should be an expiring cache
    private val receivedMessageIds = ConcurrentSkipListSet<MessageId>()

    private suspend fun handleReceivedPacket(sender: Peer, rawPacket: ByteArray) {
        withLoggingContext("sender" to sender.toString()) {
            logger.debug { "packetReceived($sender, ${rawPacket.size} bytes)" }
            val connection = connections[sender]
            if (connection != null) {
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

            } else {
                logger.debug { "Received message from unknown sender" }
            }
        }
    }

    private suspend fun handleMessage(connection: Connection, message: Message) {
        withLoggingContext("sender" to connection.peer.toString(), "message" to message::class.toString()) {
            if (message.id in receivedMessageIds) {
                logger.warn { "Disregarding message ${message.id} because it has already been received" }
            } else {
                logger.debug { "Handling message: ${message::class.simpleName}" }
                if (message !is Initiate) {
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
/*

Layers
------

1st layer, equiv to IP address.  initial join procedure should lead to small world connectivity, constant maintainence, fairly
transient connections.  This provides a search ring for layer #2.  The position on the ring is generated from a pubkey,
and the pubkey must authorize any publication to the channel.





 */