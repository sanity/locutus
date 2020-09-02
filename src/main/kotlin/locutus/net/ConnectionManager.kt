package locutus.net

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
import java.time.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.thread

/**
 * Responsible for establishing and maintaining encrypted UDP connections with remote peers.
 *
 * First message has encrypted synkey prepended, this message will be resent until it's acknowledged.
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
            logger.trace { "$sender is known" }
            connection.inboundKey.let { inboundKey ->

                if (inboundKey != null) {
                    logger.trace { "We have the inboundKey, check to see whether it's still being prepended to packet" }
                    val hasPrefix = rawPacket.startsWith(inboundKey.encryptedInboundKeyPrefix)
                    val encryptedPayload = if (hasPrefix) {
                        logger.debug { "$sender has prepended AES key although it is already known, stripping from packet" }
                        // TODO:
                        rawPacket.copyOfRange(inboundKey.encryptedInboundKeyPrefix.size, rawPacket.size)
                    } else {
                        rawPacket
                    }
                    val decryptedPayload = inboundKey.inboundKey.decrypt(encryptedPayload)
                    handleDecryptedPacket(connection, decryptedPayload)

                } else {
                    logger.trace { "We don't have the inbound key, it should be prepended to packet, decrypt it" }
                    val encryptedPrefix = rawPacket.copyOf(AESKey.RSA_ENCRYPTED_SIZE)
                    val encryptedInboundKey = RSAEncrypted(encryptedPrefix)
                    val decryptedInboundKey = AESKey(myKey.private.decrypt(encryptedInboundKey))
                    connection.inboundKey =
                        InboundKey(encryptedPrefix, decryptedInboundKey)
                    val encryptedPayload = rawPacket.copyOfRange(AESKey.RSA_ENCRYPTED_SIZE, rawPacket.size)
                    val decryptedPayload = decryptedInboundKey.decrypt(encryptedPayload)
                    handleDecryptedPacket(connection, decryptedPayload)
                }
            }
        } else {

            if (this.open) {
                logger.debug { "Opening connection to stranger" }
                val encryptedPrefix = rawPacket.copyOf(AESKey.RSA_ENCRYPTED_SIZE)
                val encryptedInboundKey = RSAEncrypted(encryptedPrefix)
                val decryptedInboundKey = AESKey(myKey.private.decrypt(encryptedInboundKey))
                val newConnection = Connection(
                    sender,
                    ConnectionType.Stranger, InboundKey(encryptedPrefix, decryptedInboundKey)
                )
                connections[sender] = newConnection
                val encryptedPayload = rawPacket.copyOfRange(AESKey.RSA_ENCRYPTED_SIZE, rawPacket.size)
                val decryptedPayload = decryptedInboundKey.decrypt(encryptedPayload)
            } else {
                logger.info("Disregarding packet from unknown sender $sender, and ConnectionManager isn't open")
            }
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

    suspend fun connect(knownPeer: KnownPeer, timeout: Duration) {
        logger.info { "Connecting to ${knownPeer.address} with timeout $timeout" }
        val giveUpTime = Instant.now().plus(timeout)
        val outboundKey = AESKey.generate()
        val encryptedOutboundKey = knownPeer.pubKey.encrypt(outboundKey.bytes)
        val outboundKeySignature = myKey.private.sign(outboundKey.bytes)
        val outboundMessage =
            ProtoBuf.encodeToByteArray(Message.serializer(), Message.Handshake(false, outboundKeySignature))
        val encryptedOutboundMessage = outboundKey.encrypt(outboundMessage)
        val outboundIntroPacket = listOf(encryptedOutboundKey.ciphertext, encryptedOutboundMessage).merge()
        val connection = Connection(knownPeer, outboundKey, false, null)
        connections[knownPeer.address] = connection
        while (!connection.outboundKeyReceived && Instant.now() < giveUpTime) {
            send(knownPeer.address, outboundIntroPacket)
            delay(openConnectionRepeatDuration)
        }
    }

    private fun send(to: SocketAddress, message: ByteArray) {
        logger.trace { "Sending ${message.size}b message to $to" }
        channel.send(ByteBuffer.wrap(message), to)
    }

    fun send(to: SocketAddress, message: Message) {
        logger.debug { "Sending $message to $to" }
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