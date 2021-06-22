package locutus.tools.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

object BigDecimalSerializer : KSerializer<BigDecimal> {

    override fun serialize(encoder: Encoder, value: BigDecimal) {

        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor


}
