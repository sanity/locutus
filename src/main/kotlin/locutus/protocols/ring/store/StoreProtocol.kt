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
import locutus.net.messages.Message.Store.Response.Type.Success
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
                    storeGetMsg.hopsToLive - 1
                )
                cm.send(from, Response(storeGetMsg.requestId, responseType))
            }
        }
    }

    suspend fun get(
        address: ContractAddress,
        getContract: Boolean,
        getPost: Boolean,
        subscribe : Subscription? = null,
        htl: Int = maxHTL,
        requestId: Int = random.nextInt()
    ) : Response.Type {
        val local = store[address.asBase58Encoded]
        return when {
            local != null -> {
                val contract = if (getContract) {
                    local.contract
                } else null
                val post = if (getPost) {
                    local.post
                } else null
                Success(contract, post)
            }
            htl <= 0 -> {
                Failure("HTL expired")
            }
            else -> {
                val deferredResponse = CompletableDeferred<Response.Type>()
                val forwardPeer = ring.ring.connectionsByDistance(address.asLocation).firstEntry()?.value
                if (forwardPeer != null) {
                    cm.listen(
                        Extractor<Response, Int>("storeResponse") { this.message.requestId },
                        requestId,
                        Duration.ofSeconds(30)
                    ) { _, storeResponse ->
                        if (storeResponse.type is Success) {
                            if (storeResponse.type.contract != null && storeResponse.type.post != null) {
                                store[address.asBase58Encoded] = ContractPost(storeResponse.type.contract, storeResponse.type.post)
                            }
                        }
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

    fun unsubscrbe(subscription : Subscription) {
        TODO()
    }

    private val subscriptions = ConcurrentHashMap<ContractAddress, MutableMap<Int, Subscription>>()

    private val subscriptionUidAddresses = ConcurrentHashMap<Int, ContractAddress>()
}

abstract class Subscription : ((Post) -> Unit) {
    val uid : Int = random.nextInt()
}