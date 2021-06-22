package locutus.apps.flog.protocol.v1

import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.ValueUpdate

@Serializable
data class FlogUpdateV1(val updated : FlogPostV1) : ValueUpdate() {

}