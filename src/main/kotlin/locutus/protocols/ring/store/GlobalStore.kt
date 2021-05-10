package locutus.protocols.ring.store

import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Value
import locutus.state.ContractPost
import locutus.state.ContractPostCache

class GlobalStore(val storeProtocol: StoreProtocol, val cpc : ContractPostCache) {
    fun put(contract : Contract, post : Value) {
        require(contract.valid(this, post))
        cpc += ContractPost(contract, post)
        TODO()
    }

    suspend fun get(contract : Contract) : Value? {
        TODO()
    }

    suspend fun get(address : ContractAddress) : ContractPost? {
        TODO()
    }

    suspend fun subscribe(address: ContractAddress, receiver : () -> Value) {
        TODO()
    }
}