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
    sealed class Assimilate : Message() {
        /**
         * Sent by joiner to gateway, requesting assimilation
         */
        class Request(val joiner : Peer?, override val respondingTo: MessageId) : Assimilate()

        class RequestSeed(val joiner : Peer, override val respondingTo: MessageId) : Assimilate()

        class RespondSeed(val seed : Long?, override val respondingTo: MessageId) : Assimilate()

        class RespondWithSeeds(val seed : Set<Long>, override val respondingTo: MessageId) : Assimilate()
    }

}

@Serializable
data class MessageId(val long : Long = random.nextLong())
