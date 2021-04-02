package locutus.protocols.ring.store

import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message.Store.Get
import locutus.net.messages.Message.Store.Response
import locutus.net.messages.Message.Store.Response.Type.Failure
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import locutus.state.ContractPostCache
import java.time.Duration

class StoreProtocol(val store : ContractPostCache, val cm: ConnectionManager, val ring : RingProtocol) {
    init {
        cm.listen<Get> { from: Peer, getMsg: Get ->
            val address = getMsg.contractAddress.asBase58Encoded
            val local = store[address]
            if (local != null) {

            } else {
                if (getMsg.hopsToLive <= 0) {
                    cm.send(from, Response(getMsg.requestId, Failure("HTL expired")))
                } else {
                    val forwardPeer = ring.ring.connectionsByDistance(getMsg.contractAddress.asLocation).firstEntry()?.value
                    if (forwardPeer != null) {
                        cm.listen(
                            Extractor<Response, Int>("storeResponse") { this.message.requestId },
                            getMsg.requestId,
                            Duration.ofSeconds(30)
                        ) { _, storeResponse ->
                            cm.send(from, Response(getMsg.requestId, storeResponse.type))
                        }
                        cm.send(forwardPeer.peerKey.peer, getMsg.copy(hopsToLive = getMsg.hopsToLive - 1))
                    } else {
                        cm.send(from, Response(getMsg.requestId, Failure("No forwarding peer")))
                    }
                }
            }
        }
    }
}