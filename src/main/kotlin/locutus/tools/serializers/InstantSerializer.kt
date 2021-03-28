package locutus.tools.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.time.Instant

object InstantSerializer : KSerializer<Instant> {

    override fun serialize(encoder: Encoder, value: Instant) {
        val surrogate = InstantSurrogate(value.epochSecond, value.nano)
        encoder.encodeSerializableValue(InstantSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Instant {
        val surrogate = decoder.decodeSerializableValue(InstantSurrogate.serializer())
        return Instant.ofEpochSecond(surrogate.seconds, surrogate.nanoSeconds.toLong())
    }

    override val descriptor: SerialDescriptor
        get() = InstantSurrogate.serializer().descriptor


}

@Serializable
@SerialName("Instant")
private class InstantSurrogate(val seconds : Long, val nanoSeconds : Int)