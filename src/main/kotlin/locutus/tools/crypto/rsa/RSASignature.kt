package locutus.tools.crypto.rsa

import kotlinx.serialization.Serializable

@Serializable
data class RSASignature(val array : ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSASignature

        if (!array.contentEquals(other.array)) return false

        return true
    }

    override fun hashCode(): Int {
        return array.contentHashCode()
    }
}