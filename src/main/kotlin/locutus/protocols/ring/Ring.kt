package locutus.protocols.ring

import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kweb.shoebox.Shoebox
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.tools.math.Location
import mu.KotlinLogging
import java.util.concurrent.ConcurrentSkipListMap

@ExperimentalSerializationApi
class Ring(private val cm: ConnectionManager, private val gateways: Set<PeerWithKey>) {

    private val logger = KotlinLogging.logger {}

    val connections = ConcurrentSkipListMap<Peer, Location>()

    @Volatile
    private var myLocation: Location? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val joinOrder = gateways.shuffled()
            for (gateway in joinOrder) {
                cm.addConnection(gateway, true)
                for (response in cm.sendAndWait(gateway.peer, Message.Ring.JoinRequest(cm.myKey.public), maxRetries = 5)) {
                    when (response) {
                        is Message.Ring.JoinAccept -> {
                            myLocation = response.yourLocation
                        }
                        is Message.Ring.AcceptJoin -> {
                            cm.addConnection(response.acceptor, false)

                        }
                    }
                }
            }
        }

    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }
}