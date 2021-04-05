package locutus.protocols.ring.store

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message.Store.StoreGet
import locutus.net.messages.Message.Store.Response
import locutus.net.messages.Message.Store.Response.Type.Failure
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import locutus.protocols.ring.contracts.ContractAddress
import locutus.state.ContractPostCache
import java.time.Duration

private val scope = MainScope()

class StoreProtocol(val store : ContractPostCache, val cm: ConnectionManager, val ring : RingProtocol, val maxHTL : Int = 10) {
    init {
        cm.listen { from: Peer, storeGetMsg: StoreGet ->
            scope.launch {
                val responseType = get(
                    storeGetMsg.contractAddress,
                    storeGetMsg.sendContract,
                    storeGetMsg.sendPost,
                    storeGetMsg.hopsToLive - 1
                )
                cm.send(from, Response(storeGetMsg.requestId, responseType))
            }
        }
    }

    suspend fun get(address : ContractAddress, getContract : Boolean, getPost : Boolean, htl : Int = maxHTL, requestId : Int = random.nextInt()) : Response.Type {
        val local = store[address.asBase58Encoded]
        return if (local != null) {
            val contract = if (getContract) {
                local.contract
            } else null
            val post = if (getPost) {
                local.post
            } else null
             Response.Type.Success(contract, post)
        } else if (htl <= 0) {
            Response.Type.Failure("HTL expired")
        } else {
            val deferredResponse = CompletableDeferred<Response.Type>()
            val forwardPeer = ring.ring.connectionsByDistance(address.asLocation).firstEntry()?.value
            if (forwardPeer != null) {
                cm.listen(
                    Extractor<Response, Int>("storeResponse") { this.message.requestId },
                    requestId,
                    Duration.ofSeconds(30)
                ) { _, storeResponse ->
                    deferredResponse.complete(storeResponse.type)
                }
                cm.send(forwardPeer.peerKey.peer, StoreGet(address, requestId, getContract, getPost, htl, TODO("subscribe?")))
            } else {
                Failure("No forwarding peer")
            }
            return deferredResponse.await()
        }
    }

}