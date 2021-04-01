package locutus.protocols.ring.store

import locutus.net.ConnectionManager
import locutus.net.messages.Message
import locutus.net.messages.Message.Store.Get
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import locutus.state.ContractPost
import locutus.state.ContractPostCache

class StoreProtocol(val store : ContractPostCache, val connectionManager: ConnectionManager, val ring : RingProtocol) {
    init {
        connectionManager.listen<Get> { from: Peer, message: Get ->

        }
    }
}