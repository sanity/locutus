package locutus.protocols

import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.ConnectionManager
import locutus.net.messages.*
import locutus.net.messages.Message.Assimilate.*
import locutus.protocols.unimatrix.*
import locutus.tools.math.Location
import java.util.concurrent.ConcurrentSkipListMap

@ExperimentalSerializationApi
class Unimatrix(private val cm: ConnectionManager, private val gateways: Set<PeerWithKey>) {

    private val adjuncts = ConcurrentSkipListMap<Location, Peer>()

    @Volatile
    private var myLocation: Location? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val joinOrder = gateways.shuffled()
            for (gateway in joinOrder) {
                cm.addConnection(gateway.peer, gateway.key, true)
                for (response in cm.sendAndWait(
                        gateway.peer,
                        GatewayRequest(cm.myKey.public),
                        maxRetries = 2
                )) {
                    when (response) {
                        is GatewayAck -> {
                            myLocation = response.yourLocation
                        }
                        is NewPeerAccept -> {

                        }
                        else -> error("Not expecting ${response::class} in response to GatewayRequest")
                    }
                }
            }
        }

    }

    private fun gatewayAccepted(gateway: PeerWithKey, response: GatewayAck) {

    }

    suspend fun <S : Any, C : Contract<S, C>> search(key: Key<S, C>) {

    }
}