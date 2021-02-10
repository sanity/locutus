package locutus.store

import kotlinx.serialization.Serializable
import kweb.shoebox.Shoebox
import kweb.shoebox.stores.MapDBStore
import locutus.net.messages.Peer
import locutus.net.messages.PeerKey

@Serializable data class GatewayDAO(
    val peerKey : PeerKey
) {
    companion object {
        val store = Shoebox(MapDBStore(MapDB.db, "gateways", serializer()))

        operator fun plusAssign(gateway : GatewayDAO) {
            store[gateway.peerKey.peer.toString()] = gateway
        }

        operator fun minusAssign(peer : Peer) {
            store.remove(peer.toString())
        }

        fun gateways(): Sequence<GatewayDAO> = store.entries.asSequence().map { it.value }

        fun gatewayPeers() : Set<PeerKey> = gateways().map { it.peerKey }.toSet()
    }
}
