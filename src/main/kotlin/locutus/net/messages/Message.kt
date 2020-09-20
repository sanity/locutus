package locutus.net.messages

import kotlinx.serialization.Serializable
import kweb.util.random
import java.security.interfaces.RSAPublicKey

@Serializable
sealed class Message {

    val id = MessageId()

    abstract val respondingTo : MessageId?

    /**
     * Assimilation
     */
    @Serializable
    data class AssimilateRequest(val joiner : Peer?, val pubKey : RSAPublicKey) : Message() {
        override val respondingTo: MessageId? = null
    }


    @Serializable
    data class AssimilateReply(override val respondingTo: MessageId?, val yourExternalAddress: Peer) : Message()
}

@Serializable
data class MessageId(val long : Long = random.nextLong())
