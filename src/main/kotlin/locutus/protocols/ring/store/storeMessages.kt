package locutus.protocols.ring.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.net.messages.Message
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Post

@Serializable
@SerialName("storeRequest")
data class StoreGet(
    /**
     * A list of addresses to request, or subscribe to, or to continue to subscribe to
     */
    val requestId : Int,
    val addresses: Map<ContractAddress, GetOptions>
) : Message() {

    @Serializable
    data class GetOptions(
        val requestContract: Boolean,
        val requestPost: Boolean,
        val subscribe: Boolean
    )
}

@Serializable
@SerialName("storeResponse")
data class StoreGetResponse(val requestId : Int, val contract : Contract?, val post : Post?) : Message() {

}

@Serializable
@SerialName("storePut")
data class StorePut(val contract : Contract, val post : Post) : Message()