package locutus.protocols.probe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.net.messages.Message
import locutus.net.messages.MessageId
import locutus.net.messages.Reply
import locutus.tools.math.Location

@Serializable
@SerialName("probeRequest")
data class ProbeRequest(val target: Location, val hopsToLive: Int) : Message()

@Serializable
@SerialName("probeResponse")
data class ProbeResponse(val visits: List<Visit>, override val replyTo: MessageId) : Message(), Reply {
    @Serializable
    data class Visit(val hop: Int, val latency: Long, val location: Location)
}