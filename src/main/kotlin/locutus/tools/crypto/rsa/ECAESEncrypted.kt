package locutus.tools.crypto.rsa

import kotlinx.serialization.Serializable

@Serializable
data class ECAESEncrypted(val encryptedAESKey: ECEncrypted, val ecEncryptedData : ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ECAESEncrypted

        if (encryptedAESKey != other.encryptedAESKey) return false
        if (!ecEncryptedData.contentEquals(other.ecEncryptedData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedAESKey.hashCode()
        result = 31 * result + ecEncryptedData.contentHashCode()
        return result
    }
}