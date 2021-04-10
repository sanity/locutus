package locutus.protocols.ring.store

import com.google.common.collect.MapMaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kweb.state.KVal
import kweb.state.KVar
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message.Store.StoreGet
import locutus.net.messages.Message.Store.Response
import locutus.net.messages.Message.Store.Response.ResponseType.Failure
import locutus.net.messages.Message.Store.Response.ResponseType.Success
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Post
import locutus.state.ContractPost
import locutus.state.ContractPostCache
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
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
                    storeGetMsg.subscribe,
                    storeGetMsg.hopsToLive - 1
                )
                cm.send(from, Response(requestId = storeGetMsg.requestId, responseType = responseType.value))
            }
        }
    }

    suspend fun get(
        address: ContractAddress,
        getContract: Boolean,
        getPost: Boolean,
        subscribe : Boolean,
        htl: Int = maxHTL,
        requestId: Int = random.nextInt()
    ) : KVal<Response.ResponseType> {
        val local = store[address.asBase58Encoded]
        return when {
            local != null -> {
                val contract = if (getContract) {
                    local.contract
                } else null
                val post = if (getPost) {
                    local.post
                } else null
                KVal(Success(contract = contract, post = post, update = false))
            }
            htl <= 0 -> {
                KVal(Failure("HTL expired"))
            }
            else -> {
                val deferredResponse = CompletableDeferred<Response.ResponseType>()
                val forwardPeer = ring.ring.connectionsByDistance(address.asLocation).firstEntry()?.value
                if (forwardPeer != null) {
                    cm.listen(
                        Extractor<Response, Int>("storeResponse") { this.message.requestId },
                        requestId,
                        Duration.ofSeconds(30)
                    ) { _, storeResponse ->
                        if (storeResponse.responseType is Success) {
                            if (storeResponse.responseType.contract != null && storeResponse.responseType.post != null) {
                                store[address.asBase58Encoded] = ContractPost(storeResponse.responseType.contract, storeResponse.responseType.post)
                            }
                        }
                        deferredResponse.complete(storeResponse.responseType)
                    }
                    cm.send(forwardPeer.peerKey.peer, StoreGet(address, requestId, getContract, getPost, htl, TODO("subscribe?")))
                } else {
                    deferredResponse.complete(Failure("No forwarding peer"))
                }
                val responseKvar = KVar(deferredResponse.await())
                if (subscribe) {
                    val id = random.nextInt()
                    // We use a map with weakvalues to allow KVals which are no-longer used to be closed automatically
                    subscriptions.computeIfAbsent(address) { MapMaker().weakValues().makeMap() }[id] = responseKvar
                    responseKvar.onClose {
                        val addressMap = subscriptions[address]
                        if (addressMap?.remove(id) != null && addressMap.isEmpty()) {
                            subscriptions.remove(address)
                        }
                    }
                }
                responseKvar
            }
        }
    }

    private val subscriptions = ConcurrentHashMap<ContractAddress, MutableMap<Int, KVar<Response.ResponseType>>>()

}

abstract class Subscription : ((Post) -> Unit) {
    val uid : Int = random.nextInt()
}