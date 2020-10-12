
package locutus.net.messages

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kweb.util.random
import locutus.tools.crypto.rsa.*
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
        class JoinRequest(val type : Type, val hopsToLive : Int) : Message(), Initiate {
            override val isInitiate = false

            @Serializable
            sealed class Type {
                @Serializable class Initial(val myPublicKey : RSAPublicKey) : Type()
                @Serializable class Proxy(val toAssimilate : PeerKeyLocation) : Type()
            }
        }

        @Serializable
        class JoinResponse(val type : Type, val acceptedBy : Set<PeerKeyLocation>) : Message() {
            @Serializable
            sealed class Type {
                @Serializable class Initial(val yourExternalAddress : Peer, val yourLocation : Location) : Type()
                @Serializable object Proxy : Type()
            }
        }

        @Serializable
        class OpenConnection(override val isInitiate: Boolean) : Message(), Initiate

        @Serializable
        class CloseConnection(val reason : String) : Message()
    }

    object Testing {
        @Serializable
        data class FooMessage(val v : Int, override val isInitiate: Boolean) : Message(), Initiate

        @Serializable
        data class BarMessage(val n : String) : Message()
    }
}

/**
 * Marker interface on a message which indicates it is initiating a connection
 * and cannot be taken as indication that we have the sender's symkey.  If this
 * marker is not present, it's assumed that the mesage is a response and the
 * sender does have our key.
 */
interface Initiate {
    val isInitiate : Boolean
}

@Serializable
data class MessageId(val long : Long = random.nextLong())
