package locutus.tools.crypto.rsa

import kotlinx.serialization.Serializable
/*
@Serializable
data class RSAAESEncrypted(val encryptedAESKey: RSAEncrypted, val rsaEncryptedData : ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSAAESEncrypted

        if (encryptedAESKey != other.encryptedAESKey) return false
        if (!rsaEncryptedData.contentEquals(other.rsaEncryptedData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedAESKey.hashCode()
        result = 31 * result + rsaEncryptedData.contentHashCode()
        return result
    }
}*/