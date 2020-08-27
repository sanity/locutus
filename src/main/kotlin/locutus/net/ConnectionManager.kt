package locutus.net

import kotlinx.coroutines.future.await
import kotlinx.coroutines.time.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.Message
import locutus.tools.crypto.*
import mu.KotlinLogging
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/**
 * Responsible for establishing and maintaining encrypted UDP connections with remote peers.
 *
 * First message has encrypted synkey prepended, this message will be resent until it's acknowledged.
 *
 */
@ExperimentalSerializationApi
class ConnectionManager(port: Int, private val myKey: RSAKeyPair, private val open: Boolean) {

    private val connections = ConcurrentHashMap<InetSocketAddress, Connection>()

    val channel: DatagramChannel

    init {
        channel = DatagramChannel.open()
        channel.socket().bind(InetSocketAddress(port))
        val buf = ByteBuffer.allocate(Constants.MAX_UDP_PACKET_SIZE + 200)
        thread { // TODO: Use non-blocking /
            while (true) {
                val sender: SocketAddress = channel.receive(buf)
                buf.flip()
                val byteArray = ByteArray(buf.remaining())
                buf.get(byteArray)
                buf.clear()
                packetReceived(sender, byteArray)
            }
        }
    }

    private fun packetReceived(sender: SocketAddress, rawPacket: ByteArray) {
        val connection = connections[sender]
        val knownSender = connection != null
        when {
            knownSender -> {
                when (val state = connection.state) {
                    is ConnectionState.Connecting -> {

                    }
                    is ConnectionState.Connected -> {
                        if (rawPacket.contentEquals(state.inboundIntroMessage)) {
                            logger.debug { "" }
                        }
                    }
                    ConnectionState.Disconnected -> TODO()
                }
            }
            open -> {

            }
            else -> {
                logger.warn { "Packet received from unknown peer $sender, ignoring" }
            }
        }
    }

    private val openConnectionRepeatDuration: Duration = Duration.ofMillis(200)

    suspend fun connect(peer: RemotePeer, timeout: Duration) {
        val giveUpTime = Instant.now().plus(timeout)
        val outboundKey = AESKey.generate()
        val encryptedOutboundKey = peer.pubKey.encrypt(outboundKey.bytes)
        val outboundKeySignature = myKey.private.sign(outboundKey.bytes)
        val outboundMessage =
            ProtoBuf.encodeToByteArray(Message.serializer(), Message.Handshake(false, outboundKeySignature))
        val encryptedOutboundMessage = outboundKey.encrypt(outboundMessage)
        val outboundIntroPacket = listOf(encryptedOutboundKey.ciphertext, encryptedOutboundMessage).merge()
        val connection = Connection(peer, outboundKey, false, null)
        connections[peer.address] = connection
        while (!connection.outboundKeyReceived && Instant.now() < giveUpTime) {
            send(peer.address, outboundIntroPacket)
            delay(openConnectionRepeatDuration)
        }
    }

    private fun send(to: SocketAddress, message: ByteArray) {
        channel.send(ByteBuffer.wrap(message), to)
    }

    fun send(to: SocketAddress, message: Message) {
        val connection = connections[to]
        requireNotNull(connection)
        val rawMessage = ProtoBuf.encodeToByteArray(Message.serializer(), message)
        val encryptedMessage = connection.outboundKey.encrypt(rawMessage)
        val keyPrepend: List<ByteArray> =
            if (connection.outboundKeyReceived) {
                emptyList()
            } else {
                listOf(connection.peer.pubKey.encrypt(connection.outboundKey.bytes).ciphertext)
            }
        val outboundRaw = (keyPrepend + encryptedMessage).merge()
        send(to, outboundRaw)
    }

    sealed class WaitForResult<M : Message> {
        data class MessageReceived<M : Message>(val message: M) : WaitForResult<M>()
        class TimedOut<M : Message> : WaitForResult<M>()
    }

    class DeferredMessageListener(
        val selector: (Message) -> Message?,
        val timeoutAt: Instant,
        val deferred: Future<WaitForResult<*>>
    )

    private val deferredMap = ConcurrentHashMap<SocketAddress, ConcurrentHashMap<Long, DeferredMessageListener>>()

    private val listenerIds = AtomicLong(0)

    suspend fun <M : Message> sendAndWait(
        from: SocketAddress,
        toSend: Message? = null,
        timeout: Duration,
        waitFor: (Message) -> M?
    ): WaitForResult<M> {
        val future = CompletableFuture<WaitForResult<*>>()
        val listener = DeferredMessageListener(waitFor, Instant.now().plus(timeout), future)
        val deferredId = listenerIds.incrementAndGet()
        deferredMap.computeIfAbsent(from) { ConcurrentHashMap() }[deferredId] = listener
        if (toSend != null) {
            send(from, toSend)
        }
        val result = future.await()
        deferredMap.getValue(from).remove(deferredId)
        @Suppress("UNCHECKED_CAST")
        return result as WaitForResult<M>
    }
}


/*

Layers
------

1st layer, equiv to IP address.  initial join procedure should lead to small world connectivity, constant maintainence, fairly
transient connections.  This provides a search ring for layer #2.  The position on the ring is generated from a pubkey,
and the pubkey must authorize any publication to the channel.





 */