package locutus.tools.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration

class DurationSerializer : KSerializer<Duration> {

    override fun serialize(encoder: Encoder, value: Duration) {
        val surrogate = DurationSurrogate(value.seconds, value.nano)
        encoder.encodeSerializableValue(DurationSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val surrogate = decoder.decodeSerializableValue(DurationSurrogate.serializer())
        return Duration.ofSeconds(surrogate.seconds, surrogate.nanoSeconds.toLong())
    }

    override val descriptor: SerialDescriptor
        get() = DurationSurrogate.serializer().descriptor


}

@Serializable
@SerialName("Duration")
private class DurationSurrogate(val seconds : Long, val nanoSeconds : Int)