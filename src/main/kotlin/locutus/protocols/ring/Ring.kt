package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consume
import kotlinx.serialization.ExperimentalSerializationApi
import kweb.util.random
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.MessageRouter.SenderMessage
import locutus.tools.math.Location
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.time.*

@ExperimentalTime
@ExperimentalSerializationApi
class Ring(private val cm: ConnectionManager, gateways: Set<PeerWithKey>) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}

    val connections = ConcurrentSkipListMap<Peer, Location>()

    @Volatile
    private var myLocation: Location? = null

    init {
        cm.listen(Extractors.JoinRequestEx, Unit, NEVER) {
            cm.send(sender, Message.Ring.JoinAccept(Location(random.nextDouble())))
        }

        for (gateway in gateways.toList().shuffled()) {
            cm.addConnection(gateway)
            cm.sendReceive(gateway.peer, Message.Ring.JoinRequest(cm.myKey.public), Extractors.JoinAcceptEx, gateway.peer, retries = 5, retryDelay = Duration.ofSeconds(1)) {

            }
        }
    }

    private suspend fun handleJoinRequest(sender : Peer, joinRequest: Message.Ring.JoinRequest) {
        cm.addConnection(PeerWithKey(sender, joinRequest.myPubKey))

    }

    private suspend fun requestJoin(gateway : PeerWithKey) {
        cm.addConnection(gateway)



        cm.send(
            gateway.peer,
            Message.Ring.JoinRequest(cm.myKey.public))
    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }

    object Extractors {
        object JoinRequestEx : MessageRouter.Extractor<Message.Ring.JoinRequest, Unit>("JoinRequest") {
            override fun invoke(message: SenderMessage<Message.Ring.JoinRequest>) = Unit
        }

        object JoinAcceptEx : MessageRouter.Extractor<Message.Ring.JoinAccept, Peer>("joinAccept") {
            override fun invoke(p1: SenderMessage<Message.Ring.JoinAccept>) = p1.sender
        }
    }
}
