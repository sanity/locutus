package locutus.tools.crypto.ec

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec

object ECPublicKeySerializer : KSerializer<ECPublicKey> {
    override val descriptor: SerialDescriptor = ECPublickKeySurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): ECPublicKey {
        val surrogate = decoder.decodeSerializableValue(ECPublickKeySurrogate.serializer())
        val spec = X509EncodedKeySpec(surrogate.encoded)
        val keyFactory = KeyFactory.getInstance("EC ")
        return keyFactory.generatePublic(spec) as ECPublicKey
    }

    override fun serialize(encoder: Encoder, value: ECPublicKey) {
        val surrogate = ECPublickKeySurrogate(value.encoded)
        encoder.encodeSerializableValue(ECPublickKeySurrogate.serializer(), surrogate)
    }

}

@Serializable
@SerialName("ECPublicKey")
private class ECPublickKeySurrogate(val encoded : ByteArray)