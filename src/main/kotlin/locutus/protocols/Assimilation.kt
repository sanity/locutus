package locutus.protocols

import locutus.net.ConnectionManager
import locutus.net.messages.Message.*
import locutus.net.messages.PeerWithKey

class Assimilation(private val cm : ConnectionManager, private val openPeers : Set<PeerWithKey>) {
    init {
        cm.listen<AssimilateRequest> { (msg, sender) ->
            cm.send(sender, AssimilateReply(msg.id, sender))
        }
    }
}