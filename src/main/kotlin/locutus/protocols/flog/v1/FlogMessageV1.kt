package locutus.protocols.flog.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class FlogMessageV1 {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : FlogMessageV1()
}