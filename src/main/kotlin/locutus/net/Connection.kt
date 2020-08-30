package locutus.net

import locutus.net.messages.Message
import locutus.tools.crypto.AESKey
import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey

 class Connection(
     val peer: RemotePeer,
     val outboundKey : AESKey,
     @Volatile var outboundKeyReceived : Boolean,
     @Volatile var inboundKey : InboundKey?
)

class InboundKey(val encryptedInboundKeyPrefix: ByteArray, val inboundKey : AESKey)

data class RemotePeer(val address: InetSocketAddress, val pubKey: RSAPublicKey)

interface MessageListener {
    fun receive(connection : Connection, message : Message)
}