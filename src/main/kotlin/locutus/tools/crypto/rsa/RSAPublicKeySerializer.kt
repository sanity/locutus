package locutus.tools.crypto.rsa

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec

object RSAPublicKeySerializer : KSerializer<RSAPublicKey> {
    override val descriptor: SerialDescriptor = RSAPublickKeySurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): RSAPublicKey {
        val surrogate = decoder.decodeSerializableValue(RSAPublickKeySurrogate.serializer())
        val spec = X509EncodedKeySpec(surrogate.encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec) as RSAPublicKey
    }

    override fun serialize(encoder: Encoder, value: RSAPublicKey) {
        val surrogate = RSAPublickKeySurrogate(value.encoded)
        encoder.encodeSerializableValue(RSAPublickKeySurrogate.serializer(), surrogate)
    }

}

@Serializable
@SerialName("RSAPublicKey")
private class RSAPublickKeySurrogate(val encoded : ByteArray)