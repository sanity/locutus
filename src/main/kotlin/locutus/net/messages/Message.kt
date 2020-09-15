package locutus.net.messages

import kotlinx.serialization.Serializable
import kweb.util.random
import locutus.tools.crypto.RSASignature

@Serializable
sealed class Message {

    val id = MessageId()

    abstract val responseTo : MessageId?

    @Serializable
    data class Hello(override val responseTo : MessageId? = null, val yourExternalAddress : Peer) : Message()

    /**
     * Assimilation
     */
    @Serializable
    data class AssimilateRequest(val assimilator : PeerWithKey)

}

@Serializable
data class MessageId(val long : Long = random.nextLong())