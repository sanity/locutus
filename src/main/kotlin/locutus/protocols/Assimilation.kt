package locutus.protocols

import locutus.net.ConnectionManager
import locutus.net.messages.PeerWithKey

class Assimilation(private val cm : ConnectionManager, private val openPeers : Set<PeerWithKey>) {
    init {
        cm.sendAndListen()
    }
}