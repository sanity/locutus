//@file:UseSerializers(RSAPubKeySerializer::class, RSAPrivateKeySerializer::class)

package locutus.tools.crypto.rsa

import kotlinx.serialization.*
import locutus.tools.crypto.AESKey
import locutus.tools.crypto.ec.ECSignature
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

fun RSAPrivateKey.sign(toSign : ByteArray) : RSASignature {
    val sig : Signature = Signature.getInstance("SHA256withRSA", "BC")
    sig.initSign(this)
    sig.update(toSign)
    return RSASignature(sig.sign())
}

fun RSAPublicKey.verify(signature : RSASignature, toVerify : ByteArray) : Boolean {
    val sig = Signature.getInstance("SHA256withRSA", "BC")
    sig.initVerify(this)
    sig.update(toVerify)
    return sig.verify(signature.array)
}

@Serializable
data class RSAEncrypted(val ciphertext : ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSAEncrypted

        if (!ciphertext.contentEquals(other.ciphertext)) return false

        return true
    }

    override fun hashCode(): Int {
        return ciphertext.contentHashCode()
    }
}

fun RSAPublicKey.encrypt(toEncrypt : ByteArray) : RSAEncrypted {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return RSAEncrypted(cipher.doFinal(toEncrypt))
}

fun RSAPrivateKey.decrypt(ciphertext : RSAEncrypted) : ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return cipher.doFinal(ciphertext.ciphertext)
}
/*
fun RSAPrivateKey.decrypt(encrypted : RSAAESEncrypted) : ByteArray {
    val aesKey = AESKey(this.decrypt(encrypted.encryptedAESKey))
    return aesKey.decrypt(encrypted.rsaEncryptedData)
}

*/