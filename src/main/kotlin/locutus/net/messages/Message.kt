package locutus.net.messages

import kotlinx.serialization.Serializable
import kweb.util.random
import locutus.tools.crypto.RSASignature
import java.net.http.HttpResponse

@Serializable
sealed class Message {

    val id = MessageId()

    abstract val respondingTo : MessageId

    @Serializable
    data class Hello(override val respondingTo: MessageId, val yourExternalAddress : Peer) : Message()

    /**
     * Assimilation
     */
    @Serializable
    data class AssimilateRequest(val assimilator : PeerWithKey)

}

@Serializable
data class MessageId(val long : Long = random.nextLong())
