package locutus.protocols.ring.store

import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Value
import locutus.protocols.ring.contracts.ValueUpdate

class GlobalStore(val storeProtocol: StoreProtocol) {
    fun update(contract : Contract, value : Value, post : ValueUpdate) {
        TODO()
    }

    suspend fun get(address : ContractAddress) : Value? {
        TODO()
    }

    suspend fun subscribe(address: ContractAddress, receiver : (Value) -> Unit) {
        TODO()
    }
}