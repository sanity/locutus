package locutus.protocols.ring

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.PeerKey
import locutus.net.messages.PeerKeyLocation
import locutus.net.sim.SimulatedNetwork
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.math.Location
import mu.KotlinLogging
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class RingProtocolSpec : FunSpec({
    context("Given a SimulatedNetwork of one node and one gateway") {
        val ringProtocols = buildNetwork(
            networkSize = 1,
            maxHopsToLive = 1,
            randomRouteHTL = 0
        )

        test("Gateway and node-0 should be connected") {
            eventually(5.seconds) {
                ringProtocols["gateway"]?.ring?.connectionsByLocation?.let { connections ->
                    connections.size shouldBe 1
                }
                ringProtocols["node-0"]?.ring?.connectionsByLocation?.let { connections ->
                    connections.size shouldBe 1
                }
            }
        }
    }

    context("Given a SimulatedNetwork of 10 nodes and one gateway") {
        val ringProtocols = buildNetwork(
            networkSize = 10,
            maxHopsToLive = 7,
            randomRouteHTL = 2
        )

        test("All nodes should connect") {

        }
    }

})

private suspend fun buildNetwork(networkSize: Int, maxHopsToLive : Int, randomRouteHTL : Int): ConcurrentHashMap<String, RingProtocol> {
    val network = SimulatedNetwork()
    val ringProtocols = ConcurrentHashMap<String, RingProtocol>()
    val gateway1Transport = network.createTransport(true, "gateway")
    val gateway1 = ConnectionManager(RSAKeyPair.create(), gateway1Transport)
    val gateway1PeerKey = PeerKey(gateway1Transport.peer, gateway1.myKey.public)
    val gatewayRingProtocol = RingProtocol(
        cm = gateway1,
        gateways = setOf(),
        myPeerKeyLocation = PeerKeyLocation(gateway1PeerKey, Location(random.nextDouble())),
        maxHopsToLive = maxHopsToLive,
        randomRouteHTL = randomRouteHTL
    )
    ringProtocols["gateway"] = gatewayRingProtocol

    val gateways = setOf(
        PeerKey(gateway1Transport.peer, gateway1.myKey.public),
    )
    for (nodeNo in 0 until networkSize) {
        logger.info {"Create node $nodeNo"}
        val peerLabel = "node-$nodeNo"
        val transport = network.createTransport(false, peerLabel)
        val connectionManager = ConnectionManager(RSAKeyPair.create(), transport)
        val ringProtocol = RingProtocol(
            connectionManager, gateways,
            maxHopsToLive = maxHopsToLive,
            randomRouteHTL = randomRouteHTL
        )
        ringProtocols[peerLabel] = ringProtocol

        delay(1000)
    }
    return ringProtocols
}
