package locutus.net.retriever

import kotlinx.serialization.KSerializer
import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message
import locutus.net.messages.Peer
import java.time.Duration
import kotlin.reflect.KClass

/**
 * Manages a request/response sequence, supporting multi-part responses
 *
 * // TODO: Handle failures
 */
abstract class Retriever<REQ : Request, RES : Response>(
    private val connectionManager: ConnectionManager,
    private val reqClass: KClass<REQ>,
    private val resClass: KClass<RES>,
    private val timeout: Duration?
) {
    init {
        connectionManager.listen(reqClass) { sender, request ->
            when(val nextStep = nextStep(request)) {
                is NextStep.Respond<REQ, RES> -> {
                    connectionManager.send(sender, nextStep.res)
                }

                is NextStep.Forward<REQ, RES> -> {
                    connectionManager.listen(
                        msgKClass = resClass,
                        extractor = Extractor("${resClass.simpleName}-retrieverId") { sender to this.message.retrieverId },
                        key = nextStep.nextPeer to request.retrieverId,
                        timeout = timeout
                    ) { _, response ->
                        connectionManager.send(sender, response)
                    }
                    connectionManager.send(nextStep.nextPeer, nextStep.req)
                }
            }
        }
    }

    abstract fun nextStep(request : REQ) : NextStep<REQ, RES>

    sealed class NextStep<REQ : Request, RES : Response> {
        class Respond<REQ : Request, RES : Response>(val res : RES) : NextStep<REQ, RES>()
        class Forward<REQ : Request, RES : Response>(val req : REQ, val nextPeer: Peer) : NextStep<REQ, RES>()
    }
}

abstract class Request : Message() {
    abstract val retrieverId : Int
}

abstract class Response : Message() {
    abstract val retrieverId : Int
}
