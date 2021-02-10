package locutus.protocols.ring

import locutus.net.messages.*
import locutus.protocols.bw.BandwidthManagementProtocol
import locutus.protocols.bw.BandwidthTracker

class PeerChooser(
    val ringProtocol: RingProtocol,
    val bandwidthTracker: BandwidthTracker,
    val bandwidthManagementProtocol: BandwidthManagementProtocol
) {
    fun choose(attempts : Int = 1, scorer : (PeerKeyLocation) -> Double) : PeerKeyLocation? {
        require(attempts > 0)
        return ringProtocol
            .connectionsByLocation.values.asSequence()
            .filter { sufficientBW(it.peerKey.peer) }
            .map { it to scorer(it) }
            .minByOrNull { it.second }
            ?.first
    }

    private fun sufficientBW(peer : Peer): Boolean {
        val rates = bandwidthTracker[peer]
        val maxBw = bandwidthManagementProtocol.getBWLimit(peer)
        return rates == null || maxBw == null || rates.max <= maxBw.bwLimit
    }
}