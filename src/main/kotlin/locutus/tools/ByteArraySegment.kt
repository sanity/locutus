package locutus.tools

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer

@Serializable data class ByteArraySegment(val array : ByteArray, val offset : Int = 0, val length : Int = array.size) : AbstractList<Byte>() {
    init {
        require(offset >= 0)
        require(length >= 0)
        require(offset + length <= array.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArraySegment

        if (!asArray.contentEquals(other.asArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return asArray.contentHashCode()
    }

    override val size: Int
        get() = length

    val asArray : ByteArray get() = array.copyOfRange(offset, offset + length)

    override fun get(index: Int): Byte = array[offset + index]

    @Serializer(forClass = ByteArraySegment::class)
    companion object : KSerializer<ByteArraySegment> {
        override val descriptor: SerialDescriptor
            get() = ByteArraySerializer().descriptor

        override fun serialize(encoder: Encoder, value: ByteArraySegment) {
            encoder.encode(ByteArraySerializer(), value.asArray)
        }

        override fun deserialize(decoder: Decoder): ByteArraySegment {
            return ByteArraySegment(decoder.decode(ByteArraySerializer()))
        }
    }


}

fun ByteArray.asSegment(): ByteArraySegment = ByteArraySegment(this)
