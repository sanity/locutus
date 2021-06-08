@file:UseSerializers(DurationSerializer::class)

package locutus.net.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import locutus.tools.serializers.DurationSerializer
import java.time.Duration


@Serializable
class Keepalive : Message()

@Serializable @SerialName("rateLimit")
data class RateLimit(val minimumMessageInterval: Duration) : Message(), CanInitiate {
    override val isInitiate = true
}

@Serializable @SerialName("multipart")
data class Multipart(val uid : Int, val thisIx : Int, val size : Int, val data : ByteArray, override val isInitiate: Boolean) : Message(), CanInitiate {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Multipart

        if (thisIx != other.thisIx) return false
        if (size != other.size) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thisIx
        result = 31 * result + size
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@Serializable @SerialName("multipartresp")
data class MultipartResponse(val partId : Int, val resendIndices : Set<Int>, override val isInitiate: Boolean) : Message(), CanInitiate


// For testing
@Serializable @SerialName("foo")
data class FooMessage(val v: Int, override val isInitiate: Boolean) : Message(), CanInitiate

@Serializable @SerialName("bar")
data class BarMessage(val n: String) : Message()