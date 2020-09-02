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
}

sealed class ConnectionType {
    data class Friend(
        val outboundKey: AESKey,
        @Volatile var outboundKeyReceived: Boolean,
        val pubKey: RSAPublicKey
    ) : ConnectionType()

    object Stranger : ConnectionType()
}

data class KnownPeer(val address: Peer, val pubKey: RSAPublicKey)

interface MessageListener {
    fun receive(connection: Connection, message: Message)
}