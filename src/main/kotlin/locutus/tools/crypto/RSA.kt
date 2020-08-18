//@file:UseSerializers(RSAPubKeySerializer::class, RSAPrivateKeySerializer::class)

package locutus.tools.crypto

import kotlinx.serialization.*
import locutus.tools.ByteArraySegment
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
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
data class RSASignature(val arraySegment : ByteArraySegment)

fun RSAPrivateKey.sign(data : ByteArray) : RSASignature {
    val sig : Signature = Signature.getInstance("SHA256withRSA", "BC")
    sig.initSign(this)
    sig.update(data)
    return RSASignature(ByteArraySegment(sig.sign()))
}

fun RSAPublicKey.verify(signature : RSASignature, data : ByteArray) : Boolean {
    val sig = Signature.getInstance("SHA256withRSA", "BC")
    sig.initVerify(this)
    sig.update(data)
    return sig.verify(signature.arraySegment.array, signature.arraySegment.offset, signature.arraySegment.length)
}

@Serializable
data class RSAEncrypted(val data : ByteArraySegment)

fun RSAPublicKey.encrypt(data : ByteArraySegment) : RSAEncrypted {
    val cipher = Cipher.getInstance("RSA/None/NoPadding", "BC")
    cipher.init(Cipher.ENCRYPT_MODE, this)
    return RSAEncrypted(ByteArraySegment(cipher.doFinal(data.array, data.offset, data.length)))
}

fun RSAPrivateKey.decrypt(encrypted : RSAEncrypted) : ByteArraySegment {
    val cipher = Cipher.getInstance("RSA/None/NoPadding", "BC")
    cipher.init(Cipher.DECRYPT_MODE, this)
    return ByteArraySegment(cipher.doFinal(encrypted.data.asArray))
}

@Serializable
data class RSAAESEncrypted(val rsaEncryptedAESKey: RSAEncrypted, val rsaEncryptedData : ByteArraySegment)

fun RSAPublicKey.encryptWithAes(data : ByteArraySegment) : RSAAESEncrypted {
    val aesKey = AESKey.generate()
    val encryptedData = aesKey.encrypt(data)
    val encryptedKey = this.encrypt(aesKey.asByteArraySegment())
    return RSAAESEncrypted(encryptedKey, encryptedData)
}

fun RSAPrivateKey.decrypt(encrypted : RSAAESEncrypted) : ByteArraySegment {
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