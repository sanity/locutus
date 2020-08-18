package locutus.tools

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable(ByteArraySegmentSerializer::class)
data class ByteArraySegment(val array : ByteArray, val offset : Int = 0, val length : Int = array.size) : AbstractList<Byte>() {
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

}

class ByteArraySegmentSerializer : KSerializer<ByteArraySegment> {
    override val descriptor: SerialDescriptor
        get() = ByteArraySerializer().descriptor

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): ByteArraySegment {
        return ByteArraySegment(ByteArraySerializer().deserialize(decoder))
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: ByteArraySegment) {
        return ByteArraySerializer().serialize(encoder, value.asArray)
    }

}

fun ByteArray.asSegment(): ByteArraySegment = ByteArraySegment(this)

fun main() {
    val bas = ByteArraySegment(byteArrayOf(1, 2, 3, 5, 8, 13))
    val serialized = ProtoBuf.encodeToByteArray(ByteArraySegment.serializer(), bas)
    val deserialized = ProtoBuf.decodeFromByteArray(ByteArraySegment.serializer(), serialized)
    println(bas == deserialized)
}