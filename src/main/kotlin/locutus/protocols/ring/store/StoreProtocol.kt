package locutus.protocols.ring.store

import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message.Store.StoreGet
import locutus.net.messages.Message.Store.Response
import locutus.net.messages.Message.Store.Response.Type.Failure
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Post
import locutus.state.ContractPost
import locutus.state.ContractPostCache
import java.time.Duration

class StoreProtocol(val store : ContractPostCache, val cm: ConnectionManager, val ring : RingProtocol) {
    init {
        cm.listen { from: Peer, storeGetMsg: StoreGet ->
            val address = storeGetMsg.contractAddress.asBase58Encoded
            val local = store[address]
            if (local != null) {
                cm.send(from, Response(storeGetMsg.requestId, Response.Type.Success(local.contract, local.post)))
            } else {
                if (storeGetMsg.hopsToLive <= 0) {
                    cm.send(from, Response(storeGetMsg.requestId, Failure("HTL expired")))
                } else {
                    val forwardPeer = ring.ring.connectionsByDistance(storeGetMsg.contractAddress.asLocation).firstEntry()?.value
                    if (forwardPeer != null) {
                        cm.listen(
                            Extractor<Response, Int>("storeResponse") { this.message.requestId },
                            storeGetMsg.requestId,
                            Duration.ofSeconds(30)
                        ) { _, storeResponse ->
                            cm.send(from, Response(storeGetMsg.requestId, storeResponse.type))
                        }
                        cm.send(forwardPeer.peerKey.peer, storeGetMsg.copy(hopsToLive = storeGetMsg.hopsToLive - 1))
                    } else {
                        cm.send(from, Response(storeGetMsg.requestId, Failure("No forwarding peer")))
                    }
                }
            }
        }
    }

    data class OptContractPost(val contract : Contract?, val post : Post?)
    fun get(address : ContractAddress, getContract : Boolean, getPost : Boolean) : OptContractPost {
        val local = store[address.asBase58Encoded]
        return if (local != null) {
            val contract = if (getContract) {
                local.contract
            } else null
            val post = if (getPost) {
                local.post
            } else null
            OptContractPost(contract, post)
        } else {

            TODO()
        }
    }

}