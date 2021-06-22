package locutus.protocols.probe

import com.google.common.base.Stopwatch
import kotlinx.coroutines.*
import locutus.net.ConnectionManager
import locutus.protocols.ring.RingProtocol
import java.util.concurrent.TimeUnit
import kotlin.math.min

class ProbeProtocol(val cm: ConnectionManager, val ringProtocol: RingProtocol) {

    private val scope = MainScope()

    companion object {
        const val maximumHopsToLive = 10
    }

    init {
        cm.assertUnique(this::class)

        cm.listen<ProbeRequest> { requestor, incomingProbeRequest ->
            val myVisit = ringProtocol.myLocation.let { myLocation ->
                requireNotNull(myLocation)
                ProbeResponse.Visit(incomingProbeRequest.hopsToLive, 0, myLocation)
            }
            if (incomingProbeRequest.hopsToLive > 0) {
                scope.launch(Dispatchers.IO) {
                    val probeResponse = probe(incomingProbeRequest)
                    cm.send(requestor, probeResponse.copy(replyTo = incomingProbeRequest.id))
                }
            } else {

                cm.send(requestor, ProbeResponse(listOf(myVisit), replyTo = incomingProbeRequest.id))
            }

        }
    }

    suspend fun probe(
        probeRequestMsg: ProbeRequest,
    ) : ProbeResponse {
        val stopwatch = Stopwatch.createStarted()
        val responseDeferred = CompletableDeferred<ProbeResponse>()
        if (ringProtocol.ring.connectionsByLocation.isEmpty()) {
            responseDeferred.completeExceptionally(NullPointerException("Ring is empty, can't forward probe"))
        } else {
            val nextPeer =
                ringProtocol.ring.connectionsByDistance(probeRequestMsg.target).firstEntry().value.peerKey.peer
            cm.send<ProbeResponse>(
                nextPeer,
                ProbeRequest(probeRequestMsg.target, min(maximumHopsToLive, probeRequestMsg.hopsToLive - 1))
            ) { sender, inboundResponse ->
                val time = stopwatch.stop().elapsed(TimeUnit.NANOSECONDS)
                val myVisit: ProbeResponse.Visit = ringProtocol.myLocation.let { myLocation ->
                    requireNotNull(myLocation)
                    ProbeResponse.Visit(probeRequestMsg.hopsToLive, time, myLocation)
                }
                val outboundResponse = inboundResponse.copy(
                    visits = listOf(myVisit) + inboundResponse.visits,
                    replyTo = probeRequestMsg.id
                )
                responseDeferred.complete(outboundResponse)
            }
        }
        return responseDeferred.await()
    }

}