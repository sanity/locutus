package locutus.protocols.microblog.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MicroblogMessageV1 {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : MicroblogMessageV1()
}