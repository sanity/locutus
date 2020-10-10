package locutus.protocols.ring

import locutus.net.messages.*
import java.time.Duration
import kotlin.system.measureTimeMillis
import kotlin.time.*

class PeerChooser(val peerSource : () -> Set<PeerKeyLocation>) {
    fun choose(criteria : Criteria, attempts : Int = 1, block : (Peer) -> ChooseResult) {
        require(attempts > 0)
        val delayMS : Duration = measureTimeMillis {
            val peer : Peer = TODO()
            block(peer)
        }.let { Duration.ofMillis(it) }
    }

    sealed class ChooseResult {
        object Success : ChooseResult()
        object Fail : ChooseResult()
    }

    sealed class Criteria {
        abstract fun score(a : PeerKeyLocation, b : PeerKeyLocation) : Double

    }
}