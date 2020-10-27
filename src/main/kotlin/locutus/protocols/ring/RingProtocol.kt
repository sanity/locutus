package locutus.protocols.ring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Ring.CloseConnection
import locutus.net.messages.Message.Ring.JoinRequest
import locutus.net.messages.Message.Ring.JoinRequest.Type.Initial
import locutus.net.messages.Message.Ring.JoinRequest.Type.Proxy
import locutus.net.messages.Message.Ring.JoinResponse
import locutus.net.messages.Message.Ring.OpenConnection
import locutus.tools.math.Location
import mu.KotlinLogging
import mu.withLoggingContext
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class RingProtocol(
    private val cm: ConnectionManager,
    private val gateways: Set<PeerKey>,
    private val maxHopsToLive: Int = 10,
    private val randomRouteHTL: Int = 3,
    @Volatile
    private var myPeerKeyLocation: PeerKeyLocation? = null
) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}

    @Volatile
    private var ring: Ring? = myPeerKeyLocation.let { pcl ->
        if (pcl == null) null else Ring(pcl.location)
    }

    init {
        handleConnectionManagerRemoveConnection()
        listenForJoinRequest()
        listenForCloseConnection()
        joinRing()

    }

    private fun handleConnectionManagerRemoveConnection() {
        cm.onRemoveConnection { peer, reason ->
            ring.let { ring ->
                requireNotNull(ring)
                ring -= peer
            }
        }
    }

    private fun listenForCloseConnection() {
        cm.listen(
            for_ = Extractor<CloseConnection, Unit>("closeConnection") { Unit },
            key = Unit,
            timeout = NEVER
        ) {
            ring.let { ring ->
                requireNotNull(ring)
                cm.removeConnection(sender, "Ring.CloseConnection received due to ${received.reason}")
            }
        }
    }

    private fun listenForJoinRequest() {
        withLoggingContext("me" to this.myPeerKeyLocation?.peerKey?.peer.toString()) {
            cm.listen(
                for_ = Extractor<JoinRequest, Unit>("joinRequest") { Unit },
                key = Unit,
                timeout = NEVER
            ) {
                val joiner = sender
                logger.debug { "JoinRequest received from $joiner" }
                val ring = ring
                requireNotNull(ring)
                val myPeerKeyLocation = myPeerKeyLocation
                requireNotNull(myPeerKeyLocation)

                val peerKeyLocation: PeerKeyLocation
                val replyType: JoinResponse.Type
                logger.debug { "JoinRequest type is ${received.type::class.simpleName}" }
                when (val type = received.type) {
                    is Initial -> {
                        peerKeyLocation = PeerKeyLocation(sender, type.myPublicKey, Location(random.nextDouble()))
                        replyType = JoinResponse.Type.Initial(peerKeyLocation.peerKey.peer, peerKeyLocation.location)
                        cm.send(sender, JoinResponse(type = replyType, replyTo = received.id))
                    }
                    is Proxy -> {
                        peerKeyLocation = type.joiner
                        replyType = JoinResponse.Type.Proxy
                    }
                }

                val acceptedBy: Set<PeerKeyLocation> = if (ring.shouldAccept(peerKeyLocation.location)) {
                    scope.launch(Dispatchers.IO) {
                        establishConnection(peerKeyLocation)
                    }
                    setOf(myPeerKeyLocation)
                } else {
                    emptySet()
                }

                val joinResponse = JoinResponse(
                    type = replyType,
                    acceptedBy = acceptedBy,
                    replyTo = received.id
                )
                cm.send(sender, joinResponse)

                if (received.hopsToLive > 0) {
                    val forwardTo = if (received.hopsToLive >= randomRouteHTL) {
                        ring.randomPeer()
                    } else {
                        ring.connectionsByDistance(peerKeyLocation.location).firstEntry().value
                    }?.peerKey?.peer

                    if (forwardTo != null) {
                        val forwarded =
                            JoinRequest(
                                type = Proxy(peerKeyLocation),
                                hopsToLive = min(received.hopsToLive, maxHopsToLive) - 1
                            )

                        val forwardedAcceptors = ConcurrentHashMap<PeerKey, Unit>()
                        acceptedBy.forEach { forwardedAcceptors[it.peerKey] = Unit }

                        cm.send<JoinResponse>(
                            to = forwardTo,
                            message = forwarded,
                            retries = 3,
                            retryDelay = Duration.ofMillis(200)
                        ) {
                            for (newAcceptor in received.acceptedBy.filter { it !in forwardedAcceptors }) {
                                cm.send(
                                    joiner,
                                    JoinResponse(JoinResponse.Type.Proxy, acceptedBy = setOf(newAcceptor), received.id)
                                )
                            }
                        }
                    } else {
                        logger.warn { "Unable to forward message from $sender because Ring is empty" }
                    }
                }
            }
        }
    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }

    private fun joinRing() {
        withLoggingContext("me" to this.myPeerKeyLocation?.peerKey?.peer.toString()) {
            logger.info { "Joining ring" }
            if (cm.transport.isOpen && gateways.isEmpty()) {
                logger.info { "No gateways to join through, but this is open so select own location" }
                this.ring = Ring(Location(random.nextDouble()))
            } else {
                for (gateway in gateways.toList().shuffled()) {
                    logger.info { "Joining Ring via $gateway" }
                    cm.addConnection(gateway, true)
                    cm.send<JoinResponse>(
                        to = gateway.peer,
                        message = JoinRequest(Initial(cm.myKey.public), maxHopsToLive),
                        retries = 5,
                        retryDelay = Duration.ofSeconds(1)
                    ) {
                        when (val type = received.type) {
                            is JoinResponse.Type.Initial -> {
                                if (ring == null) {
                                    ring = Ring(type.yourLocation)
                                }
                                if (myPeerKeyLocation == null) {
                                    myPeerKeyLocation =
                                        PeerKeyLocation(type.yourExternalAddress, cm.myKey.public, type.yourLocation)
                                }
                                ring.let { ring ->
                                    requireNotNull(ring)
                                    received.acceptedBy.forEach { newPeer ->
                                        if (ring.shouldAccept(newPeer.location)) {
                                            scope.launch {
                                                establishConnection(newPeer)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                logger.warn { "Gateway $gateway responded with incorrect message type $type" }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Initially both sides send OpenConnection(false).  When other OpenConnection(false) is received then
     * ocReceived = true, and starts sending OpenConnection(true).  When OpenConnection(true) is received, stop
     * sending.
     */
    private suspend fun establishConnection(newPeer: PeerKeyLocation) {
        withLoggingContext("me" to this.myPeerKeyLocation?.peerKey?.peer.toString()) {
            logger.info { "Establishing connection to $newPeer" }
            cm.addConnection(newPeer.peerKey, false)
            val ocReceived = AtomicBoolean(false)
            val connectionEstablished = AtomicBoolean(false)
            cm.listen(
                Extractor<OpenConnection, Peer>("openConnection") { sender },
                newPeer.peerKey.peer,
                Duration.ofSeconds(30)
            ) {
                if (received.isInitiate) {
                    logger.info { "Connection established" }
                    ocReceived.set(true)
                    connectionEstablished.set(true)
                } else {
                    logger.info { "OpenConnection received" }
                    ocReceived.set(true)
                }
            }
            scope.launch(Dispatchers.IO) {
                val giveUpTime: Instant = Instant.now() + Duration.ofSeconds(30)
                while (!connectionEstablished.get() && Instant.now() <= giveUpTime) {
                    cm.send(newPeer.peerKey.peer, OpenConnection(ocReceived.get()))
                    delay(Duration.ofMillis(200))
                }
                if (connectionEstablished.get()) {
                    ring.let { ring ->
                        requireNotNull(ring)
                        logger.info { "Add $newPeer to ring" }
                        ring += newPeer
                    }
                }
            }
        }
    }


}

data class JoinerProxy(val joiner: Peer, val proxy: Peer)