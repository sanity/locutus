package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Ring.JoinResponse
import locutus.net.messages.Message.Ring.JoinRequest
import locutus.net.messages.MessageRouter.*
import locutus.tools.math.Location
import mu.KotlinLogging
import java.time.*
import java.util.concurrent.atomic.*

class RingProtocol(
        private val cm: ConnectionManager,
        private val gateways: Set<PeerKey>,
        private val maxHopsToLive : Int = 10
) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}

    @Volatile
    private var ring: Ring? = null

    @Volatile
    private var myPeerKeyLocation : PeerKeyLocation? = null

    init {
        cm.onRemoveConnection {peer, reason ->
            ring.let {ring ->
                requireNotNull(ring)
                ring.minusAssign(peer)
            }
        }

        listenForAssimilateRequest()

        cm.listen(closeConnection, Unit, NEVER) {
            TODO()
        }

        beginAssimilation()

    }

    private fun listenForAssimilateRequest() {
        val assimilateRequestExtractor = Extractor<JoinRequest, Unit>("JoinRequest") { Unit }
        cm.listen(assimilateRequestExtractor, Unit, NEVER) {
            val ring = ring
            requireNotNull(ring)
            val myPeerKeyLocation = myPeerKeyLocation
            requireNotNull(myPeerKeyLocation)

            val peerKeyLocation: PeerKeyLocation
            val replyType: JoinResponse.Type
            when (val type = message.type) {
                is JoinRequest.Type.Initial -> {
                    peerKeyLocation = PeerKeyLocation(sender, type.myPublicKey, Location(random.nextDouble()))
                    replyType = JoinResponse.Type.Initial(peerKeyLocation.peerKey.peer, peerKeyLocation.location)
                }
                is JoinRequest.Type.Proxy -> {
                    peerKeyLocation = type.toAssimilate
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

    private fun beginAssimilation() {
        for (gateway in gateways.toList().shuffled()) {
            cm.addConnection(gateway, true)
            cm.sendReceive(
                to = gateway.peer,
                message = JoinRequest(JoinRequest.Type.Initial(cm.myKey.public), maxHopsToLive),
                extractor = joinAccept,
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
        cm.listen(openConnection, newPeer.peerKey.peer, Duration.ofSeconds(30)) {
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
                cm.send(newPeer.peerKey.peer, Message.Ring.OpenConnection(ocReceived.get()))
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



    companion object {
        val joinAccept = Extractor<JoinResponse, Peer>("joinAccept") { sender }

        val openConnection = Extractor<Message.Ring.OpenConnection, Peer>("openConnection") { sender }

        val closeConnection = Extractor<Message.Ring.CloseConnection, Unit>("closeConnection") { Unit }
    }
}
