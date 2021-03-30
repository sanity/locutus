package locutus.state

import kotlinx.serialization.Serializable
import kweb.state.KVal
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Post
import locutus.tools.math.Location
import locutus.tools.serializers.InstantSerializer
import java.time.Instant

// TODO: Distance from own location is a *prior* for request frequency

class ContractPosts(shoeboxFactory: ShoeboxFactory, myLocation : KVal<Location>) {
    private val store = shoeboxFactory.create("contract_posts", ContractPost.serializer())

    private val lastReadStore = shoeboxFactory.create("contract_posts_last_read", InstantSerializer)

    operator fun plusAssign(contractPost: ContractPost) {
        store[contractPost.contract.sig.asBase58Encoded] = contractPost
    }

    operator fun get(base58Sig : String): ContractPost? {
        val value = store[base58Sig]
        if (value != null) {
            lastReadStore[base58Sig] = Instant.now()
        }
        return value
    }

    fun deleteLeastRecentlyUsed(i : Int) {
        lastReadStore
            .entries
            .asSequence()
            .sortedBy { it.value }
            .take(i)
            .toCollection(ArrayList()).forEach { (key, value) ->
            store.remove(key)
            lastReadStore.remove(key)
        }
    }
}

@Serializable
data class ContractPost(val contract : Contract, val post : Post) {
}