package locutus.net

import locutus.tools.crypto.AESKey

data class InboundKey(val encryptedInboundKeyPrefix: ByteArray, val aesKey : AESKey) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InboundKey

        if (!encryptedInboundKeyPrefix.contentEquals(other.encryptedInboundKeyPrefix)) return false
        if (aesKey != other.aesKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedInboundKeyPrefix.contentHashCode()
        result = 31 * result + aesKey.hashCode()
        return result
    }
}