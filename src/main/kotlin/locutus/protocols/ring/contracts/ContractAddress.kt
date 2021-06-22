package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.hash
import locutus.tools.math.Location
import org.bitcoinj.core.Base58

const val ADDRESS_VERSION = 0

@Serializable
data class ContractAddress(val hash: ByteArray) {

    companion object {
        fun fromContract(contract: Contract)
        = ContractAddress(contractProtoBuf.encodeToByteArray(Contract.serializer(), contract).hash())

        fun fromBase58Encoded(encoded: String): ContractAddress {
            val decoded = Base58.decodeChecked(encoded)
            require(decoded[0].toInt() == ADDRESS_VERSION)
            return ContractAddress(decoded.sliceArray(1 until decoded.size))
        }
    }

    val asBase58Encoded: String by lazy { Base58.encodeChecked(ADDRESS_VERSION, hash) }

    val asLocation : Location by lazy {
        Location.fromByteArray(hash.asUByteArray())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContractAddress

        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }
}