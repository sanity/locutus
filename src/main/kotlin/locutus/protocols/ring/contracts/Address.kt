package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.hash
import org.bitcoinj.core.Base58

const val ADDRESS_VERSION = 0

@Serializable
data class Address(val hash: ByteArray) {

    companion object {
        fun fromContract(contract: Contract)
        = Address(ProtoBuf.encodeToByteArray(Contract.serializer(), contract).hash())

        fun fromBase58Encoded(encoded: String): Address {
            val decoded = Base58.decodeChecked(encoded)
            require(decoded[0] == ADDRESS_VERSION)
            return Address(decoded.sliceArray(1 .. decoded.size))
        }
    }

    val asBase58Encoded: String by lazy { 'F' + Base58.encodeChecked(ADDRESS_VERSION, hash) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Address

        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }
}