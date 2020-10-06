package locutus.protocols.ring

import kweb.state.KVar
import locutus.net.messages.*
import locutus.tools.math.*
import java.util.*
import java.util.concurrent.*

class Ring(val myLocation : Location) {

    val minConnections = KVar(10)

    val maxConnections = KVar(20)

    fun shouldAccept(location : Location) : Boolean {
        return when {
            connectionsByLocation.size < minConnections.value -> true
            connectionsByLocation.size >= maxConnections.value -> false
            // Placeholder for a smarter algorithm
            myLocation distance location < avgDistance -> true
            else -> false
        }
    }

    operator fun plusAssign(newPeer : PeerKeyLocation) {
        connectionsByLocation[newPeer.location] = newPeer
    }

    private val connectionsByLocation = ConcurrentSkipListMap<Location, PeerKeyLocation>()

    private val avgDistance = connectionsByDistance.keys.average()

    private val connectionsByDistance: TreeMap<Double, PeerKeyLocation>
        get() {
            return TreeMap(connectionsByLocation.mapKeys { (location, _) ->
                location.distance(myLocation) })
        }
}