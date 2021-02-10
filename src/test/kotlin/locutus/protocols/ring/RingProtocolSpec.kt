package locutus.protocols.ring

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.Message
import locutus.net.messages.PeerKey
import locutus.net.sim.SimulatedNetwork
import locutus.protocols.probe.ProbeProtocol
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.math.Location
import locutus.tools.math.distance
import mu.KotlinLogging
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class RingProtocolSpec : FunSpec({
    context("Given a SimulatedNetwork of one node and one gateway") {
        val ringProtocols = buildNetwork(
            networkSize = 1,
            ringMaxHTL = 1,
            randomIfHTLAbove = 0
        )

        test("Gateway and node-0 should be connected") {
            eventually(5.seconds) {
                ringProtocols["gateway"]?.ringProtocol?.ring?.connectionsByLocation?.let { connections ->
                    connections.size shouldBe 1
                }
                ringProtocols["node-0"]?.ringProtocol?.ring?.connectionsByLocation?.let { connections ->
                    connections.size shouldBe 1
                }
            }
        }
    }

    context("Given a network of 1000 peers") {
        val simulatedNodes = buildNetwork(
            networkSize = 200,
            ringMaxHTL = 10,
            randomIfHTLAbove = 7,
            perNodeDelay = 50
        )

        delay(60000)

        test("All nodes should have connections") {
            /*     simulatedNodes.values.forEach { simulatedNode ->
                     simulatedNode.ringProtocol.ring.let { ring ->
                         ring shouldNotBe null
                         ring.connectionsByLocation.isNotEmpty() shouldBe true
                     }
                 }
 */
            val hist: TreeMap<Double, Int> = simulatedNodes
                .values
                .map { it.ringProtocol }
                .map { ring ->
                    ring.connectionsByLocation.keys.map {
                        ring.myLocation?.distance(it) ?: error("Location unknown")
                    }
                }
                .flatten()
                .groupingBy { (it * 200.0).roundToInt().toDouble() / 200.0 }
                .eachCount()
                .toTreeMap()

            println("*** Connection Distribution")
            println("distance\tcount")
            hist.forEach { (d, c) -> println("$d\t$c") }


            for (probeIx in 0..10) {
                try {
                    val target = Location.random()
                    val probeResponse = simulatedNodes.values.random().probeProtocol.probe(
                        Message.Probe.ProbeRequest(
                            target,
                            7
                        )
                    )
                    println("Probe #$probeIx, target: $target")
                    println("hop\tlocation\tlatency")
                    for (visit in probeResponse.visits) {
                        println("${visit.hop}\t${visit.location}\t${visit.latency}")
                    }
                } catch (e : Exception) {
                    logger.error(e) { "Exception thrown during probe" }
                }
            }
        }
    }

})

private suspend fun buildNetwork(
    networkSize: Int,
    ringMaxHTL: Int,
    randomIfHTLAbove: Int,
    perNodeDelay: Long = 200
): ConcurrentHashMap<String, SimulatedNode> {
    val network = SimulatedNetwork()
    val nodes = ConcurrentHashMap<String, SimulatedNode>()
    val gateway1Transport = network.createTransport(true, "gateway")
    val gateway1 = ConnectionManager(RSAKeyPair.create(), gateway1Transport)
    val gateway1PeerKey = PeerKey(gateway1Transport.peer, gateway1.myKey.public)
    val gatewayRingProtocol = RingProtocol(
        connectionManager = gateway1,
        gateways = setOf(),
        myPeerKey = gateway1PeerKey,
        myLocation = Location(random.nextDouble()),
        maxHopsToLive = ringMaxHTL,
        randomIfHTLAbove = randomIfHTLAbove
    )

    val gatewayProbeProtocol = ProbeProtocol(cm = gateway1, gatewayRingProtocol)

    nodes["gateway"] = SimulatedNode(gatewayRingProtocol, gatewayProbeProtocol)


    val gateways = setOf(
        PeerKey(gateway1Transport.peer, gateway1.myKey.public),
    )
    for (nodeNo in 0 until networkSize) {
        logger.info { "Create node $nodeNo" }
        val peerLabel = "node-$nodeNo"
        val transport = network.createTransport(false, peerLabel)
        val connectionManager = ConnectionManager(RSAKeyPair.create(), transport)
        val ringProtocol = RingProtocol(
            connectionManager, gateways,
            maxHopsToLive = ringMaxHTL,
            randomIfHTLAbove = randomIfHTLAbove
        )
        val probeProtocol = ProbeProtocol(
            cm = connectionManager, ringProtocol = ringProtocol
        )
        nodes[peerLabel] = SimulatedNode(ringProtocol, probeProtocol)

        delay(perNodeDelay)
    }
    return nodes
}

class SimulatedNode(val ringProtocol: RingProtocol, val probeProtocol: ProbeProtocol)

private fun <K, V> Map<K, V>.toTreeMap() = TreeMap(this)