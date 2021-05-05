package locutus.protocols.flog.v1

import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.PostUpdate

@Serializable
data class FlogUpdateV1(val updated : MicroblogPostV1) : PostUpdate() {

}