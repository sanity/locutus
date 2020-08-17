package locutus.net

import locutus.tools.crypto.AESKey
import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey

 class Connection(
    val peer: RemotePeer,
    val introductoryPacketReceived: ByteArray?,
    @Volatile var state: ConnectionState
) {

}


sealed class ConnectionState {
    class Connecting(val outboundKey: AESKey, val helloMessage : ByteArray) : ConnectionState()
    class Connected(val inboundKey: AESKey, val outboundKey: AESKey) : ConnectionState()
    object Disconnected : ConnectionState()
}


data class RemotePeer(val address: InetSocketAddress, val pubKey: RSAPublicKey)
