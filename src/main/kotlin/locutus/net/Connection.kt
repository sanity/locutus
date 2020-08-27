package locutus.net

import locutus.net.messages.Message
import locutus.tools.crypto.AESKey
import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey

 class Connection(
     val peer: RemotePeer,
     val outboundKey : AESKey,
     var outboundKeyReceived : Boolean,
     var inboundKey : InboundKey?
)

class InboundKey(val encryptedInboundKeyPrefix: ByteArray, val inboundKey : AESKey)


sealed class ConnectionState {
    /**
     * Attempting to establish connection to remote peer
     */
    class Connecting(val outboundKey: AESKey) : ConnectionState()
    class Connected(val inboundKey: AESKey, val inboundIntroMessage : ByteArray, val outboundKey: AESKey) : ConnectionState()
    object Disconnected : ConnectionState()
}


data class RemotePeer(val address: InetSocketAddress, val pubKey: RSAPublicKey)

interface MessageListener {
    fun receive(connection : Connection, message : Message)
}