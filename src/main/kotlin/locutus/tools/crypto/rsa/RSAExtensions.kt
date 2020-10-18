//@file:UseSerializers(RSAPubKeySerializer::class, RSAPrivateKeySerializer::class)

package locutus.tools.crypto.rsa

import kotlinx.serialization.*
import locutus.tools.crypto.AESKey
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import javax.crypto.Cipher

fun ECPrivateKey.sign(toSign : ByteArray) : ECSignature {
    val sig : Signature = Signature.getInstance("SHA256withECDSA", "BC")
    sig.initSign(this)
    sig.update(toSign)
    return ECSignature(sig.sign())
}

fun ECPublicKey.verify(signature : ECSignature, toVerify : ByteArray) : Boolean {
    val sig = Signature.getInstance("SHA256withECDSA", "BC")
    sig.initVerify(this)
    sig.update(toVerify)
    return sig.verify(signature.array)
}

@Serializable
data class ECEncrypted(val ciphertext : ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ECEncrypted

        if (!ciphertext.contentEquals(other.ciphertext)) return false

        return true
    }

    override fun hashCode(): Int {
        return ciphertext.contentHashCode()
    }
}

fun ECPublicKey.encrypt(toEncrypt : ByteArray) : ECEncrypted {
    val cipher = Cipher.getInstance("ECGOST3410", "BC")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return ECEncrypted(cipher.doFinal(toEncrypt))
}

fun ECPrivateKey.decrypt(ciphertext : ECEncrypted) : ByteArray {
    val cipher = Cipher.getInstance("ECGOST3410", "BC")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return cipher.doFinal(ciphertext.ciphertext)
}

fun ECPublicKey.encryptWithAes(data : ByteArray) : ECAESEncrypted {
    val aesKey = AESKey.generate()
    val encryptedData = aesKey.encrypt(data)
    val encryptedKey = this.encrypt(aesKey.bytes)
    return ECAESEncrypted(encryptedKey, encryptedData)
}

fun ECPrivateKey.decrypt(encrypted : ECAESEncrypted) : ByteArray {
    val aesKey = AESKey(this.decrypt(encrypted.encryptedAESKey))
    return aesKey.decrypt(encrypted.ecEncryptedData)
}

