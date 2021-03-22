package locutus.tools.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration

class IntRangeSerializer : KSerializer<IntRange> {

    override fun serialize(encoder: Encoder, value: IntRange) {
        encoder.encodeSerializableValue(IntRangeSurrogate.serializer(), IntRangeSurrogate(value.first, value.last))
    }

    override fun deserialize(decoder: Decoder): IntRange {
        val surrogate = decoder.decodeSerializableValue(IntRangeSurrogate.serializer())
        return surrogate.first .. surrogate.last
    }

    override val descriptor: SerialDescriptor
        get() = IntRangeSurrogate.serializer().descriptor


}

@Serializable
@SerialName("IntRange")
private class IntRangeSurrogate(val first : Int, val last : Int)