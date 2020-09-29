package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consume
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.MessageRouter.SenderMessage
import locutus.tools.math.Location
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentSkipListMap

@ExperimentalSerializationApi
class Ring(private val cm: ConnectionManager, gateways: Set<PeerWithKey>) {

    private val scope = MainScope()

    private val logger = KotlinLogging.logger {}

    val connections = ConcurrentSkipListMap<Peer, Location>()

    @Volatile
    private var myLocation: Location? = null

    init {
        scope.launch(Dispatchers.IO) {
            for ((sender, joinRequest) in cm.listen(Extractors.JoinRequestEx, Unit)) {
                launch {
                    handleJoinRequest(sender, joinRequest)
                }
            }
        }
    }

    private suspend fun handleJoinRequest(sender : Peer, joinRequest: Message.Ring.JoinRequest) {
        cm.addConnection(PeerWithKey(sender, joinRequest.myPubKey))

    }

    private suspend fun requestJoin(gateway : PeerWithKey) {
        cm.addConnection(gateway)

        cm.listen(Extractors.JoinAcceptEx, gateway.peer).consume {
            for (joinAccept in this) {

            }
        }

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
