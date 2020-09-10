package locutus.net

import io.ktor.network.tls.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.time.delay
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
 * Responsible for establishing and maintaining encrypted UDP connections with remote peers.
 *
 * First message has encrypted synkey prepended, this message will be resent until it's acknowledged.
 *
 *
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

    private fun handleReceivedPacket(sender: Peer, rawPacket: ByteArray) {
        logger.debug { "packetReceived($sender, ${rawPacket.size} bytes)" }
        val connection = connections[sender]
        if (connection != null) {
            /*
            we have a connection, which means we have the decrypt key
             */

            logger.trace { "$sender is connected" }
            val encryptedPayload = connection.inboundKeyPrefix.let { inboundKeyPrefix ->
                if (inboundKeyPrefix != null && rawPacket.startsWith(inboundKeyPrefix)) {
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
        when (message) {
            is Message.Handshake -> {
                TODO()
            }
            is Message.HandshakeResponse -> {
                TODO()
            }
            else -> {
                TODO()
            }
        }
    }

    private val openConnectionRepeatDuration: Duration = Duration.ofMillis(200)

    sealed class ConnectResult {
        object Connected : ConnectResult()
        object TimedOut : ConnectResult()
    }

    /**
     * @param knownPeer The peer to connect to
     * @param isOpen Is [knownPeer] open?
     * @param timeout How long to attempt to connect
     */
    suspend fun connect(
        peer: Peer,
        pubKey: RSAPublicKey,
        isOpen: Boolean,
        timeout: Duration
    ): ConnectResult {

        logger.info { "Connecting to ${peer} with timeout $timeout" }
        val giveUpTime = Instant.now().plus(timeout)
        val outboundKey = AESKey.generate()
        val encryptedOutboundKey = pubKey.encrypt(outboundKey.bytes)
        val outboundKeySignature = myKey.private.sign(outboundKey.bytes)
        val outboundMessage =
            ProtoBuf.encodeToByteArray(Message.serializer(), TLSRecordType.Handshake(false, outboundKeySignature))
        val encryptedOutboundMessage = outboundKey.encrypt(outboundMessage)
        val outboundIntroPacket = listOf(encryptedOutboundKey.ciphertext, encryptedOutboundMessage).merge()
        val connection = Connection(peer, false, outboundKey, if (isOpen) outboundKey else null, null)
        connections[peer] = connection
        while (!connection.outboundKeyReceived && Instant.now() < giveUpTime) {
            send(peer, outboundIntroPacket)
            delay(openConnectionRepeatDuration)
        }
        return when {
            Instant.now() >= giveUpTime -> {
                connections.remove(knownPeer.peer)
                ConnectResult.TimedOut
            }
            type.outboundKeyReceived -> {
                logger.info { "OutboundKeyReceived, connected" }
                ConnectResult.Connected
            }
            else -> {
                error("Unknown connection result")
            }
        }
    }

    private fun send(to: Peer, message: ByteArray) {
        logger.trace { "Sending ${message.size}b message to $to" }
        channel.send(ByteBuffer.wrap(message), to.asSocketAddress)
    }

    fun send(to: SocketAddress, message: Message) {
        logger.debug { "Sending $message to $to" }
        val connection = connections[to]
        requireNotNull(connection)
        val rawMessage = ProtoBuf.encodeToByteArray(Message.serializer(), message)
        val encryptedMessage = connection.packetEncryptKey.encrypt(rawMessage)
        val keyPrepend: List<ByteArray> =
            if (connection.outboundKeyReceived) {
                emptyList()
            } else {
                listOf(connection.peer.pubKey.encrypt(connection.packetEncryptKey.bytes).ciphertext)
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

    suspend fun <M : Message, EXT : Any> sendAndWait(
        from: SocketAddress,
        toSend: Message? = null,
        timeout: Duration,
        extractor : (M) -> EXT,
        extracted : EXT,
        filter: (Message) -> M?
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