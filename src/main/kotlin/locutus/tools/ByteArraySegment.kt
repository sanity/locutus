package locutus.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId
import locutus.TrConstants
import java.io.*
import java.net.DatagramPacket
import java.util.*

/*
 * TODO: Use custom serializer to avoid serializing unused parts of `array`
 */

@Serializable
class ByteArraySegment(@ProtoId(1) val array: ByteArray,
                       @ProtoId(2) val offset: Int,
                       @ProtoId(3) val length: Int) : Iterable<Byte?> {

    init {
        require(offset + length <= array.size)
        { "offset ($offset) + length ($length) must be <= array.size (${array.size})" }
    }

    fun startsWith(other: ByteArraySegment): Boolean {
        if (other.length > length) return false
        for (x in 0 until other.length) {
            if (byteAt(x) != other.byteAt(x)) return false
        }
        return true
    }

    constructor(array: ByteArray) : this(array, 0, array.size)

    fun toBAIS(): ByteArrayInputStream {
        return ByteArrayInputStream(array, offset, length)
    }

    fun toDataInputStream(): DataInputStream {
        return DataInputStream(toBAIS())
    }

    fun writeTo(os: OutputStream) {
        os.write(array, offset, length)
    }

    fun subsegment(offset: Int, length: Int = Int.MAX_VALUE): ByteArraySegment {
        return ByteArraySegment(array, this.offset + offset,
            kotlin.math.min(length, array.size - (this.offset + offset))
        )
    }

    class ByteArraySegmentBuilder : DataOutputStream(ByteArrayOutputStream(TrConstants.DEFAULT_BAOS_SIZE)) {
        fun write(seg: ByteArraySegment) {
            try {
                this.write(seg.array, seg.offset, seg.length)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        fun build(): ByteArraySegment {
            return try {
                flush()
                val baos = out as ByteArrayOutputStream
                ByteArraySegment(baos.toByteArray())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + Arrays.hashCode(array)
        result = prime * result + length
        result = prime * result + offset
        return result
    }

    fun byteAt(pos: Int): Byte {
        if (pos > length) throw ArrayIndexOutOfBoundsException("byteAt($pos) but length is $length")
        return array[offset + pos]
    }

    override fun toString(): String {
        val ret = StringBuffer()
        ret.append("ByteArraySegment[length=$length data=[")
        ret.append(this.joinToString(separator = ","))
        ret.append("]")
        return ret.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is ByteArraySegment) return false
        val other = other
        if (length != other.length) return false
        for (x in 0 until length) {
            if (byteAt(x) != other.byteAt(x)) return false
        }
        return true
    }

    override fun iterator(): Iterator<Byte> {
        return sequence {
            for (ix in 0..length) {
                yield(byteAt(ix))
            }
        }.iterator()
    }

    companion object {
        fun from(dp: DatagramPacket): ByteArraySegment {
            val array = ByteArray(dp.length)
            // Create defensive copy of array to ensure immutability
            System.arraycopy(dp.data, dp.offset, array, 0, dp.length)
            return ByteArraySegment(array)
        }

        @Throws(IOException::class)
        fun from(`is`: InputStream, maxLength: Int): ByteArraySegment {
            val ba = ByteArray(maxLength)
            val len = `is`.read(ba)
            assert(`is`.read() == -1)
            return ByteArraySegment(ba, 0, len)
        }

        fun builder(): ByteArraySegment.ByteArraySegmentBuilder {
            return ByteArraySegment.ByteArraySegmentBuilder()
        }
    }
}

fun ByteArray.asSegment(): ByteArraySegment {
    return ByteArraySegment(this, 0, this.size)
}
