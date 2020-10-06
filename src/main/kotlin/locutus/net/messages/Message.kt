
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

    object Ring {
        @Serializable
        class AssimilateRequest(val assimilatorPublicKey : RSAPublicKey, val assimilatorPeer : Peer?) : Message(), Initiate {
            override val hasYourKey = false
        }

        @Serializable
        class AssimilateReply(val yourLocation : Location, val yourPeer : Peer?, val acceptedBy : Set<PeerKeyLocation>) : Message()

        @Serializable
        class OpenConnection(override val hasYourKey: Boolean) : Message(), Initiate

        @Serializable
        class CloseConnection(val reason : String) : Message()
    }

    object Testing {
        @Serializable
        data class FooMessage(val v : Int) : Message()

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
    val hasYourKey : Boolean
}

@Serializable
data class MessageId(val long : Long = random.nextLong())
