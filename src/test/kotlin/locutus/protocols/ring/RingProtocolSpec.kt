package locutus.protocols.ring

import io.kotest.core.spec.style.FunSpec
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.PeerKey
import locutus.net.messages.PeerKeyLocation
import locutus.net.sim.SimulatedNetwork
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.math.Location

class RingProtocolSpec : FunSpec({
    val networkSize = 2
    context("Given a SimulatedNetwork of $networkSize nodes") {
        val network = SimulatedNetwork()
        val gateway1Transport = network.createTransport(true)
        val gateway1 = ConnectionManager(RSAKeyPair.create(), gateway1Transport)
        val gateway1PeerKey = PeerKey(gateway1Transport.peer, gateway1.myKey.public)
        RingProtocol(
            cm = gateway1,
            gateways = setOf(),
            myPeerKeyLocation = PeerKeyLocation(gateway1PeerKey, Location(random.nextDouble())),
            maxHopsToLive = 2,
            randomRouteHTL = 1
        )
        val gateways = setOf(
            PeerKey(gateway1Transport.peer, gateway1.myKey.public),
        )
        for (nodeNo in 0 .. networkSize) {
            val transport = network.createTransport(false)
            val connectionManager = ConnectionManager(RSAKeyPair.create(), transport)
            val ringProtocol = RingProtocol(connectionManager, gateways)
        }
    }

})
