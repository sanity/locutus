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

// For testing
@Serializable @SerialName("foo")
data class FooMessage(val v: Int, override val isInitiate: Boolean) : Message(), CanInitiate

@Serializable @SerialName("bar")
data class BarMessage(val n: String) : Message()