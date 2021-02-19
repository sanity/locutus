package locutus.protocols.ring.store

import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Post

class RingStore {
}

@Serializable
class ContractPost(val contract : Contract, val post : Post)