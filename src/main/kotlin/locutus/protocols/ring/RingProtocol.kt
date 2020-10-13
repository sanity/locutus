package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Ring.CloseConnection
import locutus.net.messages.Message.Ring.JoinResponse
import locutus.net.messages.Message.Ring.JoinRequest
import locutus.net.messages.Message.Ring.JoinRequest.Type.Initial
import locutus.net.messages.Message.Ring.OpenConnection
import locutus.tools.math.Location
import mu.KotlinLogging
import java.time.*
import java.util.concurrent.atomic.*

class RingProtocol(
        private val cm: ConnectionManager,
        private val gateways: Set<PeerKey>,
        private val maxHopsToLive : Int = 10,
        private val randomRouteHTL : Int = 3
) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}

    @Volatile
    private var ring: Ring? = null

    @Volatile
    private var myPeerKeyLocation : PeerKeyLocation? = null

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
                cm.removeConnection(sender, "Ring.CloseConnection received due to ${message.reason}")
            }
        }
    }

    private fun listenForJoinRequest() {
        cm.listen(
                for_ = Extractor<JoinRequest, Unit>(label = "JoinRequest") { Unit },
                key = Unit,
                timeout = NEVER
        ) {
            val ring = ring
            requireNotNull(ring)
            val myPeerKeyLocation = myPeerKeyLocation
            requireNotNull(myPeerKeyLocation)

            val peerKeyLocation: PeerKeyLocation
            val replyType: JoinResponse.Type
            when (val type = message.type) {
                is Initial -> {
                    peerKeyLocation = PeerKeyLocation(sender, type.myPublicKey, Location(random.nextDouble()))
                    replyType = JoinResponse.Type.Initial(peerKeyLocation.peerKey.peer, peerKeyLocation.location)
                    cm.send(sender, JoinResponse(replyType, emptySet()))
                }
                is JoinRequest.Type.Proxy -> {
                    peerKeyLocation = type.joiner
                    replyType = JoinResponse.Type.Proxy
                }
            }

            val acceptedBy : Set<PeerKeyLocation>
            if (ring.shouldAccept(peerKeyLocation.location)) {
                acceptedBy = setOf(myPeerKeyLocation)
            }
        }
    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }

    private fun joinRing() {
        for (gateway in gateways.toList().shuffled()) {
            cm.addConnection(gateway, true)
            cm.sendReceive(
                to = gateway.peer,
                message = JoinRequest(Initial(cm.myKey.public), maxHopsToLive),
                extractor = Extractor<JoinResponse, Peer>("joinAccept") { sender },
                key = gateway.peer,
                retries = 5,
                retryDelay = Duration.ofSeconds(1)
            ) {
                when(val type = message.type) {
                    is JoinResponse.Type.Initial -> {
                        if (ring == null) {
                            ring = Ring(type.yourLocation)
                        }
                        if (myPeerKeyLocation == null) {
                            myPeerKeyLocation = PeerKeyLocation(type.yourExternalAddress, cm.myKey.public, type.yourLocation)
                        }
                        ring.let { ring ->
                            requireNotNull(ring)
                            for (newPeer in message.acceptedBy) {
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

    /**
     * Initially both sides send OpenConnection(false).  When other OpenConnection(false) is received then
     * ocReceived = true, and starts sending OpenConnection(true).  When OpenConnection(true) is received, stop
     * sending.
     */
    private suspend fun establishConnection(newPeer : PeerKeyLocation) {
        cm.addConnection(newPeer.peerKey, false)
        val ocReceived = AtomicBoolean(false)
        val connectionEstablished = AtomicBoolean(false)
        cm.listen(
                Extractor<OpenConnection, Peer>("openConnection") { sender },
                newPeer.peerKey.peer,
                Duration.ofSeconds(30)
        ) {
            if (message.isInitiate) {
                ocReceived.set(true)
                connectionEstablished.set(true)
            } else {
                ocReceived.set(true)
            }
        }
        scope.launch {
            val giveUpTime : Instant = Instant.now() + Duration.ofSeconds(30)
            while (!connectionEstablished.get() && Instant.now() <= giveUpTime) {
                cm.send(newPeer.peerKey.peer, OpenConnection(ocReceived.get()))
                delay(Duration.ofMillis(200))
            }
            if (connectionEstablished.get()) {
                ring.let { ring ->
                    requireNotNull(ring)
                    ring += newPeer
                }
            }
        }
    }


}
