package locutus.net

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import locutus.Constants
import locutus.net.messages.Message
import locutus.tools.crypto.AESKey
import locutus.tools.crypto.encrypt
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Responsible for establishing and maintaining encrypted UDP connections with remote peers.
 *
 * First message has encrypted synkey prepended, this message will be resent until it's acknowledged.
 *
 */
class ConnectionManager(val port: Int, val open: Boolean) {

    private val connections = ConcurrentHashMap<InetSocketAddress, Connection>()

    init {
        val channel = DatagramChannel.open()
        channel.socket().bind(InetSocketAddress(port))
        val buf = ByteBuffer.allocate(Constants.MAX_UDP_PACKET_SIZE + 200)
        GlobalScope.launch {
            while (true) {
                val sender: SocketAddress = channel.receive(buf)
                buf.flip()
                val byteArray = ByteArray(buf.remaining())
                buf.get(byteArray)
                buf.clear()
                launch {
                    packetReceived(sender, byteArray)
                }
            }
        }
    }

    private fun packetReceived(sender: SocketAddress, message: ByteArray) {
        val connection = connections[sender]
        if (connection != null) {

        } else if (open) {

        }
    }

    suspend fun connect(peer: RemotePeer) {
        val outboundKey = AESKey.generate()
        val encryptedOutboundKey = peer.pubKey.encrypt(outboundKey.asByteArraySegment()).data
        val outboundMessage = ProtoBuf Message.OpenConnection(1)
      //  val existingConnection = connections.computeIfAbsent(peer.address) { Connection(peer, null, null, ConnectionState.Connecting(AESKey.generate())) }

    }
}



/*

Layers
------

1st layer, equiv to IP address.  initial join procedure should lead to small world connectivity, constant maintainence, fairly
transient connections.  This provides a search ring for layer #2.  The position on the ring is generated from a pubkey,
and the pubkey must authorize any publication to the channel.





 */