package locutus.protocols.ring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kweb.state.KVar
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Ring.CloseConnection
import locutus.net.messages.Message.Ring.JoinRequest
import locutus.net.messages.Message.Ring.JoinRequest.Type.Initial
import locutus.net.messages.Message.Ring.JoinRequest.Type.Proxy
import locutus.net.messages.Message.Ring.JoinResponse
import locutus.net.messages.Message.Ring.OpenConnection
import locutus.net.messages.Message.Ring.OpenConnection.ConnectionState
import locutus.net.messages.Message.Ring.OpenConnection.ConnectionState.*
import locutus.tools.math.Location
import mu.KotlinLogging
import mu.withLoggingContext
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.math.min

private val logger = KotlinLogging.logger {}

class RingProtocol(
    private val cm: ConnectionManager,
    private val gateways: Set<PeerKey>,
    private val maxHopsToLive: Int = 10,
    private val randomIfHTLAbove: Int = maxHopsToLive - 3,
    @Volatile
    var myPeerKey: PeerKey? = null,
    @Volatile
    var myLocation: Location? = null

) {

    var myPeerKeyLocation : PeerKeyLocation? get() {
        return myPeerKey.let { myPeerKey ->
            myLocation.let {myLocation ->
                requireNotNull(myPeerKey)
                requireNotNull(myLocation)
                PeerKeyLocation(myPeerKey, myLocation)
            }
        }
    }
    set(value) {
        requireNotNull(value)
        myPeerKey = value.peerKey
        myLocation = value.location
    }


    private val scope = MainScope()

    val ring: Ring = Ring()

    init {
        handleConnectionManagerRemoveConnection()
        listenForJoinRequest()
        listenForCloseConnection()
        joinRing()

    }

    val connectionsByLocation: ConcurrentSkipListMap<Location, PeerKeyLocation>
        get() = ring.connectionsByLocation

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
        withLoggingContext("me" to this.myPeerKey?.peer.toString()) {
            cm.listen(
                for_ = Extractor<JoinRequest, Unit>("joinRequest") { Unit },
                key = Unit,
                timeout = NEVER
            ) {
                val joinRequestSender = sender
                val joinRequestId = received.id

                logger.debug { "JoinRequest received from $sender with HTL ${this.received.hopsToLive} of type ${received.type::class.simpleName}" }
                val ring = ring
                requireNotNull(ring)

                val peerKeyLocation: PeerKeyLocation
                logger.debug { "JoinRequest type is ${received.type::class.simpleName}" }
                val replyType = when (val type = received.type) {
                    is Initial -> {
                        peerKeyLocation = PeerKeyLocation(sender, type.myPublicKey, Location(random.nextDouble()))
                        JoinResponse.Type.Initial(peerKeyLocation.peerKey.peer, peerKeyLocation.location)
                    }
                    is Proxy -> {
                        peerKeyLocation = type.joiner
                        JoinResponse.Type.Proxy
                    }
                }

                val acceptedBy: Set<PeerKeyLocation> = run {
                    myPeerKeyLocation.let { myPeerKeyLocation ->
                        requireNotNull(myPeerKeyLocation)
                        if (ring.shouldAccept(
                                myPeerKeyLocation.location,
                                peerKeyLocation.location
                            )
                        ) {
                            logger.debug { "Accepting connection to ${peerKeyLocation.peerKey.peer}, establishing connection" }
                            scope.launch(Dispatchers.IO) {
                                establishConnection(peerKeyLocation)
                            }
                            setOf(myPeerKeyLocation)
                        } else {
                            logger.debug { "Not accepting new connection from ${peerKeyLocation.peerKey.peer}" }
                            emptySet()
                        }
                    }
                }

                val joinResponse = JoinResponse(
                    type = replyType,
                    acceptedBy = acceptedBy,
                    replyTo = joinRequestId
                )
                logger.debug { "Sending joinResponse to $sender accepting ${acceptedBy.size} connections." }
                cm.send(joinRequestSender, joinResponse)

                if (received.hopsToLive > 0 && ring.connectionsByLocation.size > 0) {
                    // TODO: Need unified way to exclude peers from consideration
                    val forwardTo = if (received.hopsToLive >= randomIfHTLAbove) {
                        logger.info { "Randomly selecting peer to forward JoinRequest from $sender" }
                        ring.randomPeer(exclude = listOf {
                            it.peerKey.peer == sender
                        })
                    } else {
                        logger.info { "Selecting closest peer to forward request from $sender" }
                        ring.connectionsByDistance(peerKeyLocation.location)
                            .filterValues { it.peerKey.peer != sender }
                            .entries
                            .firstOrNull()?.value
                    }?.peerKey?.peer

                    if (forwardTo != null) {
                        val forwarded =
                            JoinRequest(
                                type = Proxy(peerKeyLocation),
                                hopsToLive = min(received.hopsToLive, maxHopsToLive) - 1
                            )

                        val forwardedAcceptors = ConcurrentHashMap<PeerKey, Unit>()
                        acceptedBy.forEach { forwardedAcceptors[it.peerKey] = Unit }

                        logger.info { "Forwarding JoinRequest from $sender to $forwardTo" }
                        cm.send<JoinResponse>(
                            to = forwardTo,
                            message = forwarded,
                            retries = 3,
                            retryDelay = Duration.ofMillis(200)
                        ) {
                            val newAcceptors = received.acceptedBy.filter { it !in forwardedAcceptors }.toSet()
                            newAcceptors.forEach { forwardedAcceptors[it.peerKey] = Unit }
                            cm.send(
                                joinRequestSender,
                                JoinResponse(JoinResponse.Type.Proxy, acceptedBy = newAcceptors, joinRequestId)
                            )
                        }
                    }
                } else {
                    logger.warn { "Unable to forward message from $sender with HTL ${received.hopsToLive} because Ring is empty" }
                }
            }
        }
    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }

    private fun joinRing() {
        withLoggingContext("me" to this.myPeerKey?.peer.toString()) {
            if (cm.transport.isOpen && gateways.isEmpty()) {
                logger.info { "No gateways to join through, but this is open so select own location" }
            } else {
                for (gateway in gateways.toList().shuffled()) {
                    logger.info { "Joining Ring via $gateway" }
                    cm.addConnection(gateway, true)
                    val joinRequest = JoinRequest(Initial(cm.myKey.public), maxHopsToLive)
                    logger.debug { "Sending JoinRequest(id=${joinRequest.id}) to ${gateway.peer}" }
                    cm.send<JoinResponse>(
                        to = gateway.peer,
                        message = joinRequest,
                        retries = 5,
                        retryDelay = Duration.ofSeconds(1)
                    ) {
                        logger.debug { "JoinResponse received from $sender of type ${received.type::class.simpleName}" }
                        when (val type = received.type) {
                            is JoinResponse.Type.Initial -> {
                                if (myPeerKey == null) {
                                    myPeerKey =
                                        PeerKey(type.yourExternalAddress, cm.myKey.public)
                                    logger.info { "Gateway has informed me that my PeerKey is $myPeerKey" }
                                }
                                myLocation = type.yourLocation
                            }
                        }

                        received.acceptedBy.forEach { newPeer ->
                            if (ring.shouldAccept(
                                    myLocation ?: error("Can't accept $newPeer because myLocation is unknown"),
                                    newPeer.location
                                )
                            ) {
                                logger.info { "Establishing connection to $newPeer" }
                                scope.launch(Dispatchers.IO) {
                                    establishConnection(newPeer)
                                }
                            } else {
                                logger.debug { "Not accepting connection to $newPeer" }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun establishConnection(newPeer: PeerKeyLocation) {
        withLoggingContext(
            "me" to this.myPeerKey?.peer.toString(),
            "newPeer" to newPeer.peerKey.peer.toString()
        ) {
            cm.addConnection(newPeer.peerKey, false)
            val myState: KVar<ConnectionState> = KVar(Connecting)

            myState.addListener { oldState, newState ->
                if (oldState != Connected && newState == Connected) {
                    logger.info { "${this@RingProtocol.myPeerKey}(${this@RingProtocol.myLocation?.value}) connected to $newPeer, adding to ring" }
                    ring += newPeer
                }
            }

            cm.listen(
                Extractor<OpenConnection, Peer>("openConnection") { sender },
                newPeer.peerKey.peer,
                Duration.ofSeconds(30)
            ) {

                when (received.myState) {
                    Connecting -> {
                        myState.value = OCReceived
                    }
                    OCReceived -> {
                        myState.value = Connected
                    }
                    Connected -> {
                        myState.value = Connected
                    }
                }

                if (received.myState != Connected) {
                    val openConnection =
                        OpenConnection(myState = myState.value)
                    logger.debug { "Acklowledging OC: $openConnection" }
                    cm.send(newPeer.peerKey.peer, openConnection)
                }
            }

            scope.launch(Dispatchers.IO) {
                val giveUpTime: Instant = Instant.now() + Duration.ofSeconds(30)
                while (myState.value != Connected && Instant.now() <= giveUpTime) {
                    val openConnection =
                        OpenConnection(myState = myState.value)
                    logger.debug { "Sending $openConnection to $newPeer" }
                    cm.send(newPeer.peerKey.peer, openConnection)
                    delay(Duration.ofMillis(200))
                }
            }
        }
    }


}

data class JoinerProxy(val joiner: Peer, val proxy: Peer)