package locutus.net

import locutus.net.messages.Message
import locutus.tools.crypto.AESKey
import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey

 class Connection(
    val peer: RemotePeer,
    @Volatile var state: ConnectionState
) {

}


sealed class ConnectionState {
    /**
     * Attempting to establish connection to remote peer
     */
    class Connecting(val outboundKey: AESKey, val outboundIntroMessage : ByteArray) : ConnectionState()
    class Connected(val inboundKey: AESKey, val inboundIntroMessage : ByteArray, val outboundKey: AESKey) : ConnectionState()
    object Disconnected : ConnectionState()
}


data class RemotePeer(val address: InetSocketAddress, val pubKey: RSAPublicKey)

interface MessageListener {
    fun receive(connection : Connection, message : Message)
}