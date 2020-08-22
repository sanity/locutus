package locutus.net

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.Constants
import locutus.net.messages.Message
import locutus.tools.crypto.AESKey
import locutus.tools.crypto.encrypt
import locutus.tools.crypto.merge
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/**
 * Responsible for establishing and maintaining encrypted UDP connections with remote peers.
 *
 * First message has encrypted synkey prepended, this message will be resent until it's acknowledged.
 *
 */
@ExperimentalSerializationApi
class ConnectionManager(val port: Int, val open: Boolean) {

    private val connections = ConcurrentHashMap<InetSocketAddress, Connection>()

    init {
        val channel = DatagramChannel.open()
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
        if (connection != null) {
            when(val state = connection.state) {
                is ConnectionState.Connecting -> {

                }
                is ConnectionState.Connected -> {
                    if ( rawPacket.contentEquals(state.inboundIntroMessage)) {
                        logger.debug { "" }
                    }
                }
                ConnectionState.Disconnected -> TODO()
            }
        } else if (open) {

        } else {
            logger.warn { "Packet received from unknown peer $sender, ignoring" }
        }
    }

    suspend fun connect(peer: RemotePeer) {
        val outboundKey = AESKey.generate()
        val encryptedOutboundKey = peer.pubKey.encrypt(outboundKey.bytes)
        val outboundMessage = ProtoBuf.encodeToByteArray(Message.serializer(), Message.OpenConnection(1))
        val encryptedOutboundMessage = outboundKey.encrypt(outboundMessage)
        val outboundIntroPacket = listOf(encryptedOutboundKey.ciphertext, encryptedOutboundMessage).merge()
        val connection = Connection(peer, ConnectionState.Connecting(outboundKey, outboundIntroPacket))
        connections[peer.address] = connection

        //  val existingConnection = connections.computeIfAbsent(peer.address) { Connection(peer, null, null, Connecti
        //
        //onState.Connecting(AESKey.generate())) }

    }

    fun send(to: InetSocketAddress, message: Message) {

    }
}


/*

Layers
------

1st layer, equiv to IP address.  initial join procedure should lead to small world connectivity, constant maintainence, fairly
transient connections.  This provides a search ring for layer #2.  The position on the ring is generated from a pubkey,
and the pubkey must authorize any publication to the channel.





 */