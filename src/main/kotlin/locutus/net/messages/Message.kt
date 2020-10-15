
package locutus.net.messages

import kotlinx.serialization.*
import kweb.util.random
import locutus.tools.math.Location
import java.security.interfaces.RSAPublicKey

@Serializable
sealed class Message {

    /**
     * Used for detecting duplicate messages
     */
    val id = MessageId()

    @Serializable
    class Keepalive() : Message()

    object Ring {
        @Serializable
        class JoinRequest(val type : Type, val hopsToLive : Int) : Message(), CanInitiate {
            override val isInitiate = false

            @Serializable
            sealed class Type {
                @Serializable class Initial(val myPublicKey : RSAPublicKey) : Type()
                @Serializable class Proxy(val joiner : PeerKeyLocation) : Type()
            }
        }

        @Serializable
        class JoinResponse(val type : Type, val joiner : Peer, val acceptedBy : Set<PeerKeyLocation>, override val replyTo: MessageId) : Message(), Reply {
            @Serializable
            sealed class Type {
                @Serializable class Initial(val yourExternalAddress : Peer, val yourLocation : Location) : Type()
                @Serializable object Proxy : Type()
            }
        }

        @Serializable
        class OpenConnection(override val isInitiate: Boolean) : Message(), CanInitiate

        @Serializable
        class CloseConnection(val reason : String) : Message()
    }

    object Testing {
        @Serializable
        data class FooMessage(val v : Int, override val isInitiate: Boolean) : Message(), CanInitiate

        @Serializable
        data class BarMessage(val n : String) : Message()
    }
}

/**
 * Marker interface on a message which indicates it may initiate a connection,
 * and therefore can't be assumed to confirm the sender has our outbound symkey.
 *
 * [isInitiate] must also return true for this to be the case.
 */
interface CanInitiate {
    val isInitiate : Boolean
}

interface Reply {
    val replyTo : MessageId
}

@Serializable
data class MessageId(val long : Long = random.nextLong())
