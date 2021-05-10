package locutus.state

import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.Bytes
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Value
import locutus.tools.serializers.InstantSerializer
import java.time.Instant

class ContractPostCache(shoeboxFactory: ShoeboxFactory, val maxSize : Bytes) {
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

    operator fun set(base58Sig: String, cp : ContractPost) {
        store[base58Sig] = cp
    }
}

@Serializable
data class ContractPost(val contract : Contract, val post : Value) {
}