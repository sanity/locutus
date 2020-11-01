package locutus.protocols.ring

import kweb.state.KVar
import locutus.net.messages.Peer
import locutus.net.messages.PeerKeyLocation
import locutus.tools.math.Location
import locutus.tools.math.distance
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap


class Ring(val myLocation: Location) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    val minConnections = KVar(10)

    val maxConnections = KVar(20)

    fun shouldAccept(location: Location): Boolean {
        return when {
            connectionsByLocation.size < minConnections.value -> true
            connectionsByLocation.size >= maxConnections.value -> false
            // Placeholder for a smarter algorithm
            myLocation distance location < medianDistanceToMe -> true
            else -> false
        }
    }

    operator fun plusAssign(newPeer: PeerKeyLocation) {
        connectionsByLocation[newPeer.location] = newPeer
    }

    operator fun minusAssign(peer: Peer) {
        val connectionByLocation = connectionsByLocation.filterValues { it.peerKey.peer == peer }.entries.firstOrNull()
        if (connectionByLocation == null) {
            logger.warn { "Attempted to remove peer $peer, but it's not in the Ring" }
        } else {
            connectionsByLocation.remove(connectionByLocation.value.location)
        }
    }

    fun randomPeer(exclude : List<(PeerKeyLocation) -> Boolean>) =
        connectionsByLocation
            .values
            .filter { exclude.none { filter -> filter(it) } }
            .randomOrNull()

    val connectionsByLocation = ConcurrentSkipListMap<Location, PeerKeyLocation>()

    private val medianDistanceToMe get() = connectionsByDistance(myLocation).keys.sorted().get(connectionsByLocation.size / 2)

    fun connectionsByDistance(to: Location): TreeMap<Double, PeerKeyLocation> {
        return TreeMap(connectionsByLocation.mapKeys { (location, _) ->
            location.distance(to)
        })
    }

}