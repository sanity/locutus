package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Ring.AssimilateReply
import locutus.net.messages.MessageRouter.SenderMessage
import mu.KotlinLogging
import java.time.*
import java.util.concurrent.atomic.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalSerializationApi
class RingProtocol(private val cm: ConnectionManager, private val gateways: Set<PeerKey>) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}


    @Volatile
    private var ring: Ring? = null

    init {
        cm.listen(Extractors.AssimilateRequestExtractor, Unit, NEVER) {
            TODO()
        }

        cm.listen(Extractors.CloseConnectionEx, Unit, NEVER) {

        }

        beginAssimilation()

    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }

    private fun beginAssimilation() {
        for (gateway in gateways.toList().shuffled()) {
            cm.addConnection(gateway)
            cm.sendReceive(
                to = gateway.peer,
                message = Message.Ring.AssimilateRequest(cm.myKey.public, null),
                extractor = Extractors.JoinAcceptEx,
                key = gateway.peer,
                retries = 5,
                retryDelay = Duration.ofSeconds(1)
            ) {
                if (ring == null) {
                    ring = Ring(message.yourLocation)
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
        }
    }

    /**
     * Initially both sides send OpenConnection(false).  When other OpenConnection(false) is received then
     * ocReceived = true, and starts sending OpenConnection(true).  When OpenConnection(true) is received, stop
     * sending.
     */
    private suspend fun establishConnection(newPeer : PeerKeyLocation) {
        cm.addConnection(newPeer.peerKey)
        val ocReceived = AtomicBoolean(false)
        val connectionEstablished = AtomicBoolean(false)
        cm.listen(Extractors.OpenConnectionEx, newPeer.peerKey.peer, Duration.ofSeconds(30)) {
            if (message.hasYourKey) {
                ocReceived.set(true)
                connectionEstablished.set(true)
            } else {
                ocReceived.set(true)
            }
        }
        scope.launch {
            val giveUpTime = Instant.now() + Duration.ofSeconds(30)
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



    object Extractors {
        object AssimilateRequestExtractor : MessageRouter.Extractor<Message.Ring.AssimilateRequest, Unit>("JoinRequest") {
            override fun invoke(message: SenderMessage<Message.Ring.AssimilateRequest>) = Unit
        }

        object JoinAcceptEx : MessageRouter.Extractor<AssimilateReply, Peer>("joinAccept") {
            override fun invoke(p1: SenderMessage<AssimilateReply>) = p1.sender
        }

        object OpenConnectionEx : MessageRouter.Extractor<Message.Ring.OpenConnection, Peer>("openConnection") {
            override fun invoke(p1: SenderMessage<Message.Ring.OpenConnection>) = p1.sender
        }

        object CloseConnectionEx : MessageRouter.Extractor<Message.Ring.CloseConnection, Unit>("closeConnection") {
            override fun invoke(p1: SenderMessage<Message.Ring.CloseConnection>) = Unit
        }
    }
}
