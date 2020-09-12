package locutus.net

import kotlinx.coroutines.future.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.*
import locutus.tools.crypto.*
import mu.KotlinLogging
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.interfaces.RSAPublicKey
import java.time.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread

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
            val decryptedPayload = connection.inboundKey.decrypt(encryptedPayload)
            handleDecryptedPacket(connection, decryptedPayload)

        } else {
            logger.trace { "Received message from unknown sender" }
        }
    }

    private fun handleDecryptedPacket(connection: Connection, packet: ByteArray) {
        val message = ProtoBuf.decodeFromByteArray(Message.serializer(), packet)
        logger.debug { "Handling message: ${message::class.simpleName}" }
        if (message.isResponse) {
            connection.outboundKeyReceived = true
        }
    }

    private val openConnectionRepeatDuration: Duration = Duration.ofMillis(200)

    sealed class ConnectResult {
        object Connected : ConnectResult()
        object TimedOut : ConnectResult()
    }

    /**
     * @param peer The peer to connect to
     * @param isOpen Is [peer] open?
     */
    fun connect(
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

    fun sendAndListen(to: Peer, message: Message) {
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
        channel.send(ByteBuffer.wrap(outboundRaw), to.asSocketAddress)
    }

    sealed class SendResult {
        data class MessageReceived(val message: Message) : SendResult()
        object TimedOut : SendResult()
    }

    class DeferredMessageListener(
        val timeoutAt: Instant,
        val future: Future<SendResult>
    )

    private val deferredMap = ConcurrentHashMap<SocketAddress, ConcurrentHashMap<MessageId, DeferredMessageListener>>()

    private val listenerIds = AtomicLong(0)

    suspend fun sendAndListen(
        to : Peer,
        message: Message,
        retryEvery : Duration = Duration.ofMillis(200),
        retries : Int = 5
    ): SendResult {
        val future = CompletableFuture<SendResult>()
        val listener = DeferredMessageListener(waitFor, Instant.now().plus(timeout), future)
        val deferredId = listenerIds.incrementAndGet()
        deferredMap.computeIfAbsent(from) { ConcurrentHashMap() }[deferredId] = listener
        if (toSend != null) {
            sendAndListen(from, toSend)
        }
        val result = future.await()
        deferredMap.getValue(from).remove(deferredId)
        @Suppress("UNCHECKED_CAST")
        return result as SendResult<M>
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