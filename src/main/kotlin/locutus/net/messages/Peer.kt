@file:UseSerializers(RSAPublicKeySerializer::class)

package locutus.net.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
import locutus.tools.math.Location
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.interfaces.RSAPublicKey
import java.text.DecimalFormat

@Serializable
data class Peer(val addr: ByteArray, val port: Int, val label: String? = null) {
    constructor(socketAddress: InetSocketAddress, label: String? = null) :
            this(socketAddress.address.address, socketAddress.port, label)

    val asSocketAddress: InetSocketAddress by lazy {
        InetSocketAddress(InetAddress.getByAddress(addr), port)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (!addr.contentEquals(other.addr)) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = addr.contentHashCode()
        result = 31 * result + port
        return result
    }

    override fun toString(): String = label ?: asSocketAddress.toString()
}

@Serializable
data class PeerKey(val peer: Peer, val key: RSAPublicKey) {
    override fun toString(): String {
        return peer.toString()
    }
}

@Serializable
data class PeerKeyLocation(val peerKey: PeerKey, val location: Location) {
    companion object {
        private val locationFormat = DecimalFormat("#.000")
    }

    constructor(peer: Peer, key: RSAPublicKey, location: Location) :
            this(PeerKey(peer, key), location)

    override fun toString(): String {
        return if (peerKey.peer.label != null) {
            "${peerKey.peer.label}(${ locationFormat.format(location.value)})"
        } else {
            super.toString()
        }
    }
}