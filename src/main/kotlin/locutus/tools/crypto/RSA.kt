//@file:UseSerializers(RSAPubKeySerializer::class, RSAPrivateKeySerializer::class)

package locutus.tools.crypto

import kotlinx.serialization.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

@Serializable
data class RSAKeyPair(val public : RSAPublicKey, val private : RSAPrivateKey) {
    companion object {

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun create() : RSAKeyPair {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val key = keyGen.generateKeyPair()
            return RSAKeyPair(key.public as RSAPublicKey, key.private as RSAPrivateKey)
        }
    }
}

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

fun RSAPrivateKey.sign(data : ByteArray) : RSASignature {
    val sig : Signature = Signature.getInstance("SHA256withRSA", "BC")
    sig.initSign(this)
    sig.update(data)
    return RSASignature(sig.sign())
}

fun RSAPublicKey.verify(signature : RSASignature, bytes : ByteArray) : Boolean {
    val sig = Signature.getInstance("SHA256withRSA", "BC")
    sig.initVerify(this)
    sig.update(bytes)
    return sig.verify(signature.array)
}

@Serializable
data class RSAEncrypted(val bytes : ByteArray)

fun RSAPublicKey.encrypt(bytes : ByteArray) : RSAEncrypted {
    val cipher = Cipher.getInstance("RSA/None/NoPadding", "BC")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return RSAEncrypted(cipher.doFinal(bytes))
}

fun RSAPrivateKey.decrypt(encrypted : RSAEncrypted) : ByteArray {
    val cipher = Cipher.getInstance("RSA/None/NoPadding", "BC")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return cipher.doFinal(encrypted.bytes)
}

@Serializable
data class RSAAESEncrypted(val rsaEncryptedAESKey: RSAEncrypted, val rsaEncryptedData : ByteArray)

fun RSAPublicKey.encryptWithAes(data : ByteArray) : RSAAESEncrypted {
    val aesKey = AESKey.generate()
    val encryptedData = aesKey.encrypt(data)
    val encryptedKey = this.encrypt(aesKey.bytes)
    return RSAAESEncrypted(encryptedKey, encryptedData)
}

fun RSAPrivateKey.decrypt(encrypted : RSAAESEncrypted) : ByteArray {
    val aesKey = AESKey(this.decrypt(encrypted.rsaEncryptedAESKey))
    return aesKey.decrypt(encrypted.rsaEncryptedData)
}
/*
object RSAPubKeySerializer : KSerializer<RSAPublicKey> {
    override val descriptor: SerialDescriptor
        = PrimitiveDescriptor("RSAPubKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RSAPublicKey {
        val key = Base64.getDecoder().decode(decoder.decodeString())
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(key)) as RSAPublicKey
    }

    override fun serialize(encoder: Encoder, value: RSAPublicKey) {
       encoder.encodeString(Base64.getEncoder().encodeToString(value.encoded))
    }

}
object RSAPrivateKeySerializer : KSerializer<RSAPrivateKey> {
    override val descriptor: SerialDescriptor
            = PrimitiveDescriptor("RSAPrivateKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RSAPrivateKey {
        val key = Base64.getDecoder().decode(decoder.decodeString())
        return KeyFactory.getInstance("RSA").generatePrivate(X509EncodedKeySpec(key)) as RSAPrivateKey
    }

    override fun serialize(encoder: Encoder, value: RSAPrivateKey) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value.encoded))
    }

}

 */