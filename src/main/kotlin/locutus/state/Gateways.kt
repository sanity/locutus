package locutus.state

import kotlinx.serialization.Serializable
import kweb.shoebox.Shoebox
import kweb.shoebox.stores.MapDBStore
import locutus.net.messages.Peer
import locutus.net.messages.PeerKey
import org.mapdb.DB

class Gateways(shoeboxFactory: ShoeboxFactory) {
    private val store = shoeboxFactory.create("gateways", Gateway.serializer())

    operator fun plusAssign(gateway : Gateway) {
        store[gateway.peerKey.peer.toString()] = gateway
    }

    operator fun minusAssign(peer : Peer) {
        store.remove(peer.toString())
    }

    fun all(): Sequence<Gateway> = store.entries.asSequence().map { it.value }

    fun gatewayPeers() : Set<PeerKey> = all().map { it.peerKey }.toSet()

    @Serializable data class Gateway(
        val peerKey : PeerKey
    )
}


