package locutus.protocols.ring.store

import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Post
import locutus.state.ContractPost

class GlobalStore {
    fun put(contract : Contract, post : Post) {
        TODO()
    }

    suspend fun get(contract : Contract) : Post? {
        TODO()
    }

    suspend fun get(address : ContractAddress) : ContractPost? {
        TODO()
    }

    class Subscription() {
        suspend fun await() : Post {
            TODO()
        }

        fun unsubscribe() {
            TODO()
        }
    }
}