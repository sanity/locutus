package locutus.protocols.ring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kweb.state.KVar
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
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
    private val connectionManager: ConnectionManager,
    private val gateways: Set<PeerKey>,
    private val maxHopsToLive: Int = 10,
    private val randomIfHTLAbove: Int = maxHopsToLive - 3,
    @Volatile
    var myPeerKey: PeerKey? = null,
    @Volatile
    var myLocation: Location? = null

) {

    var myPeerKeyLocation: PeerKeyLocation?
        get() {
            return myPeerKey.let { myPeerKey ->
                myLocation.let { myLocation ->
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
        connectionManager.assertUnique(this::class)

        handleConnectionManagerRemoveConnection()
        listenForJoinRequest()
        listenForCloseConnection()
        joinRing()

    }

    val connectionsByLocation: ConcurrentSkipListMap<Location, PeerKeyLocation>
        get() = ring.connectionsByLocation

    private fun handleConnectionManagerRemoveConnection() {
        connectionManager.onRemoveConnection { peer, reason ->
            ring.let { ring ->
                requireNotNull(ring)
                ring -= peer
            }
        }
    }

    private fun listenForCloseConnection() {
        connectionManager.listen<CloseConnection> { sender, msg ->
            connectionManager.removeConnection(sender, "Ring.CloseConnection received due to ${msg.reason}")
        }
    }

    private fun listenForJoinRequest() {
        withLoggingContext("me" to this.myPeerKey?.peer.toString()) {
            connectionManager.listen<JoinRequest> { sender, joinRequest ->

                logger.debug { "JoinRequest received sender $sender with HTL ${joinRequest.hopsToLive} of type ${joinRequest.type::class.simpleName}" }
                val ring = ring

                val peerKeyLocation: PeerKeyLocation
                logger.debug { "JoinRequest type is ${joinRequest.type::class.simpleName}" }
                val replyType = when (val type = joinRequest.type) {
                    is JoinRequest.Type.Initial -> {
                        peerKeyLocation = PeerKeyLocation(sender, type.myPublicKey, Location(random.nextDouble()))
                        JoinResponse.Type.Initial(peerKeyLocation.peerKey.peer, peerKeyLocation.location)
                    }
                    is JoinRequest.Type.Proxy -> {
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
                            logger.debug { "Not accepting new connection sender ${peerKeyLocation.peerKey.peer}" }
                            emptySet()
                        }
                    }
                }

                val joinResponse = JoinResponse(
                    type = replyType,
                    acceptedBy = acceptedBy,
                    replyTo = joinRequest.id
                )
                logger.debug { "Sending joinResponse to $sender accepting ${acceptedBy.size} connections." }
                connectionManager.send(sender, joinResponse)

                if (joinRequest.hopsToLive > 0 && ring.connectionsByLocation.size > 0) {
                    // TODO: Need unified way to exclude peers sender consideration
                    val forwardTo = if (joinRequest.hopsToLive >= randomIfHTLAbove) {
                        logger.info { "Randomly selecting peer to forward JoinRequest sender $sender" }
                        ring.randomPeer(exclude = listOf {
                            it.peerKey.peer == sender
                        })
                    } else {
                        logger.info { "Selecting closest peer to forward request sender $sender" }
                        ring.connectionsByDistance(peerKeyLocation.location)
                            .filterValues { it.peerKey.peer != sender }
                            .entries
                            .firstOrNull()?.value
                    }?.peerKey?.peer

                    if (forwardTo != null) {
                        val forwarded =
                            JoinRequest(
                                type = JoinRequest.Type.Proxy(peerKeyLocation),
                                hopsToLive = min(joinRequest.hopsToLive, maxHopsToLive) - 1
                            )

                        val forwardedAcceptors = ConcurrentHashMap<PeerKey, Unit>()
                        acceptedBy.forEach { forwardedAcceptors[it.peerKey] = Unit }

                        logger.info { "Forwarding JoinRequest sender $sender to $forwardTo" }
                        connectionManager.send<JoinResponse>(
                            to = forwardTo,
                            message = forwarded,
                            retries = 3,
                            retryDelay = Duration.ofMillis(200)
                        ) { jrSender, joinResponse ->
                            val newAcceptors = joinResponse.acceptedBy.filter { it !in forwardedAcceptors }.toSet()
                            newAcceptors.forEach { forwardedAcceptors[it.peerKey] = Unit }
                            connectionManager.send(
                                jrSender,
                                JoinResponse(JoinResponse.Type.Proxy, acceptedBy = newAcceptors, joinRequest.id)
                            )
                        }
                    }
                } else {
                    logger.warn { "Unable to forward message sender $sender with HTL ${joinRequest.hopsToLive} because Ring is empty" }
                }
            }
        }
    }

    private fun joinRing() {
        withLoggingContext("me" to this.myPeerKey?.peer.toString()) {
            if (connectionManager.transport.isOpen && gateways.isEmpty()) {
                logger.info { "No gateways to join through, but this is open so select own location" }
            } else {
                for (gateway in gateways.toList().shuffled()) {
                    logger.info { "Joining Ring via $gateway" }
                    connectionManager.addConnection(gateway, true)
                    val joinRequest = JoinRequest(JoinRequest.Type.Initial(connectionManager.myKey.public), maxHopsToLive)
                    logger.debug { "Sending JoinRequest(id=${joinRequest.id}) to ${gateway.peer}" }
                    connectionManager.send<JoinResponse>(
                        to = gateway.peer,
                        message = joinRequest,
                        retries = 5,
                        retryDelay = Duration.ofSeconds(1)
                    ) { sender, joinResponse ->
                        logger.debug { "JoinResponse received from $sender of type ${joinResponse.type::class.simpleName}" }
                        when (val type = joinResponse.type) {
                            is JoinResponse.Type.Initial -> {
                                if (myPeerKey == null) {
                                    myPeerKey =
                                        PeerKey(type.yourExternalAddress, connectionManager.myKey.public)
                                    logger.info { "Gateway has informed me that my PeerKey is $myPeerKey" }
                                }
                                myLocation = type.yourLocation
                            }
                        }

                        joinResponse.acceptedBy.forEach { newPeer ->
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
            connectionManager.addConnection(newPeer.peerKey, false)
            val myState: KVar<ConnectionState> = KVar(Connecting)

            myState.addListener { oldState, newState ->
                if (oldState != Connected && newState == Connected) {
                    logger.info { "${this@RingProtocol.myPeerKey}(${this@RingProtocol.myLocation?.value}) connected to $newPeer, adding to ring" }
                    ring += newPeer
                }
            }

            connectionManager.listen(
                Extractor<OpenConnection, Peer>("openConnection") { sender },
                newPeer.peerKey.peer,
                Duration.ofSeconds(30)
            ) { _, openConnectionMsg ->

                when (openConnectionMsg.myState) {
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

                if (openConnectionMsg.myState != Connected) {
                    val openConnection =
                        OpenConnection(myState = myState.value)
                    logger.debug { "Acklowledging OC: $openConnection" }
                    connectionManager.send(newPeer.peerKey.peer, openConnection)
                }
            }

            scope.launch(Dispatchers.IO) {
                val giveUpTime: Instant = Instant.now() + Duration.ofSeconds(30)
                while (myState.value != Connected && Instant.now() <= giveUpTime) {
                    val openConnection =
                        OpenConnection(myState = myState.value)
                    logger.debug { "Sending $openConnection to $newPeer" }
                    connectionManager.send(newPeer.peerKey.peer, openConnection)
                    delay(Duration.ofMillis(200))
                }
            }
        }
    }


}

data class JoinerProxy(val joiner: Peer, val proxy: Peer)