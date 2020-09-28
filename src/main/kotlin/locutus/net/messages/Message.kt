
package locutus.net.messages

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kweb.util.random
import locutus.tools.crypto.rsa.*
import locutus.tools.math.Location
import java.security.interfaces.RSAPublicKey

@Serializable
sealed class Message {

    val id = MessageId()

    abstract val respondingTo : MessageId?

    object Ring {
        @Serializable
        class JoinRequest(val myPubKey : RSAPublicKey) : Message() {
            override val respondingTo: MessageId? = null
        }

        @Serializable
        class JoinAccept(override val respondingTo: MessageId, val yourLocation : Location) : Message()

        @Serializable
        class ReferJoin(val joiner : PeerWithKey, val location : Location) : Message() {
            override val respondingTo: MessageId? = null
        }

        @Serializable
        class AcceptJoin(override val respondingTo: MessageId, val acceptor : PeerWithKey) : Message()

        @Serializable
        class Join() : Message() {
            override val respondingTo: MessageId? = null
        }
    }
}

@Serializable
data class MessageId(val long : Long = random.nextLong())
