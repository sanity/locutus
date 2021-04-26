package locutus.protocols.microblog.v1

import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.PostUpdate

@Serializable
data class MicroblogUpdateV1(val updated : MicroblogPostV1) : PostUpdate() {

}