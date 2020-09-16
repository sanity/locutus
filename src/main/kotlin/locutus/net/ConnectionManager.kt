package locutus.net

import com.google.common.cache.*
import com.google.common.collect.MapMaker
import com.google.gson.annotations.Since
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.time.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.*
import locutus.tools.crypto.*
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.function.*
import kotlin.concurrent.thread
import kotlin.reflect.KClass

/**
 * [key]Hello(false, extAddr) ->
 */
@ExperimentalSerializationApi
class ConnectionManager(val port: Int, val myKey: RSAKeyPair, private val open: Boolean) {

    private val logger = KotlinLogging.logger {}

    private val connections = ConcurrentHashMap<Peer, Connection>()

    private val channel: DatagramChannel = DatagramChannel.open()

    init {
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
                handleReceivedPacket(sender, byteArray)
            }
        }
    }

    /*
        Connection exists, inboundKey known
        Connection exists, inboundKey unknown
     */

    private fun handleReceivedPacket(sender: Peer, rawPacket: ByteArray) {
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
            handleDecryptedPacket(connection, decryptedPayload)

        } else {
            logger.debug { "Received message from unknown sender" }
        }
    }

    private fun handleDecryptedPacket(connection: Connection, packet: ByteArray) {
        val message = ProtoBuf.decodeFromByteArray(Message.serializer(), packet)

        messagesSentInResponse.getIfPresent(message.id).let { sentInResponse ->
            if (sentInResponse != null && sentInResponse.isNotEmpty()) {
                
            }
        }

        logger.debug { "Handling message: ${message::class.simpleName}" }
        if (message.responseTo != null) {
            logger.trace { "Message is response to ${message.responseTo}" }
            if (!connection.outboundKeyReceived) connection.outboundKeyReceived = true
            val listener = responseListeners.getIfPresent(message.responseTo)
                ?: error("No response listener found for reply message $message")
            listener.complete(SendResult.MessageReceived(message))
        } else {
            logger.trace { "Message is not response" }
            val listener = this.inboundMessageListeners[message::class]
            if (listener != null) {
                listener.accept(message)
            } else {
                logger.warn { "Received unknown message type ${message::class}, disregarding" }
            }
        }
    }

    /**
     * @param peer The peer to connect to
     * @param isOpen Is [peer] open?
     */
    fun addConnection(
        peer: Peer,
        pubKey: RSAPublicKey,
        isOpen: Boolean
    ) {
        require(!connections.containsKey(peer)) { "Connection to $peer already exists" }

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

    fun send(to: Peer, message: Message) {
        logger.debug { "Sending $message to $to" }
        val connection = connections[to]
        requireNotNull(connection)
        val serializedMessage = ProtoBuf.encodeToByteArray(Message.serializer(), message)
        val encryptedMessage = connection.outboundKey.encrypt(serializedMessage)
        val keyPrepend: List<ByteArray> =
            if (connection.outboundKeyReceived) {
                emptyList()
            } else {
                listOf(connection.encryptedOutboundKeyPrefix)
            }
        val outboundRaw = (keyPrepend + encryptedMessage).merge()
        logger.trace { "Sending ${outboundRaw.size}b message to $to" }
        if (message.responseTo != null) {
            // TODO: Don't want to re-add messages if they've already been sent
            messagesSentInResponse[message.responseTo] += message
        }
        channel.send(ByteBuffer.wrap(outboundRaw), to.asSocketAddress)
    }

    private val inboundMessageListeners = ConcurrentHashMap<KClass<Message>, Consumer<Message>>()

    inline fun <reified M : Message> listen(listener : Consumer<M>) {
        this.listen(M::class, listener)
    }

    @PublishedApi
    internal fun listen(msgClass : KClass<out Message>, listener : Consumer<out Message>) {
        require(!inboundMessageListeners.containsKey(msgClass)) { "Listener already present for message type $msgClass" }
        inboundMessageListeners[msgClass as KClass<Message>] = listener as Consumer<Message>
    }

    private val responseListeners : Cache<MessageId, SendChannel<Message>> =
        CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(5))
            .removalListener<MessageId, SendChannel<Message>> { rn ->
                rn.value.close()
            }
            .build()

    private val messagesSentInResponse :  LoadingCache<MessageId, ConcurrentLinkedQueue<Message>> =
        CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(5))
            .build( object : CacheLoader<MessageId, ConcurrentLinkedQueue<Message>>() {
                override fun load(key: MessageId): ConcurrentLinkedQueue<Message> {
                    return ConcurrentLinkedQueue()
                }

            } )

    fun sendAndListen(
        to: Peer,
        message: Message
    ): ReceiveChannel<Message> {
        val channel = Channel<Message>()
        responseListeners.put(message.id, channel)
        send(to, message)
        return channel
    }

    suspend fun sendWithRetry(to : Peer, message : Message, retryEvery : Duration = Duration.ofMillis(200), maxRetries : Int = 5) : ReceiveChannel<Message> {
        val responseReceived = AtomicBoolean(false)
        val replyChannel = sendAndListen(to, message)
        val wrappedReplyChannel = Channel<Message>()
        GlobalScope.launch {
            for (m in replyChannel) {
                if (!responseReceived.get()) responseReceived.set(true)
                wrappedReplyChannel.send(m)
            }
        }
        for (attempt in 1..maxRetries) {
            if (responseReceived.get()) {
                break
            }
            send(to, message)
        }

        return wrappedReplyChannel
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