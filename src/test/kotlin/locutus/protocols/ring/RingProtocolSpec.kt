package locutus.protocols.ring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import locutus.net.ConnectionManager
import locutus.net.messages.PeerKey
import locutus.net.sim.SimulatedNetwork
import locutus.tools.crypto.rsa.RSAKeyPair

class RingProtocolSpec : FunSpec({
    val networkSize = 10
    context("Given a SimulatedNetwork of $networkSize nodes") {
        val network = SimulatedNetwork()
        val gatewayTransport = network.createTransport(true)
        val gateway = ConnectionManager(RSAKeyPair.create(), gatewayTransport)
        val gateways = setOf(PeerKey(gatewayTransport.peer, gateway.myKey.public))
        for (nodeNo in 0 .. networkSize) {
            val transport = network.createTransport(false)
            val connectionManager = ConnectionManager(RSAKeyPair.create(), transport)
            val ringProtocol = RingProtocol(connectionManager, gateways)
        }
    }

})
