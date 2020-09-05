package locutus.net

import locutus.net.messages.*
import locutus.tools.crypto.AESKey
import java.security.interfaces.RSAPublicKey

class Connection(
    val peer: Peer,
    val type: ConnectionType,
    @Volatile var inboundKey: InboundKey?,
) {
    val receivedInbound get() = inboundKey != null

    val outboundKey: AESKey
        get() {
            return when (type) {
                is ConnectionType.Friend -> type.outboundKey
                ConnectionType.Stranger -> inboundKey.let {
                    it?.aesKey ?: error("Stranger has no inboundKey so can't encrypt outbound message")
                }
            }
        }
}

sealed class ConnectionType {
    data class Friend(
        val outboundKey: AESKey,
        @Volatile var outboundKeyReceived: Boolean,
        val pubKey: RSAPublicKey
    ) : ConnectionType()

    object Stranger : ConnectionType()
}

data class KnownPeer(val peer: Peer, val pubKey: RSAPublicKey)

interface MessageListener {
    fun receive(connection: Connection, message: Message)
}