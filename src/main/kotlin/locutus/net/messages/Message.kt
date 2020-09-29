
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
    class Ack() : Message()

    object Ring {
        @Serializable
        class JoinRequest(val myPubKey : RSAPublicKey) : Message(), Initiate

        @Serializable
        class JoinAccept( val yourLocation : Location) : Message()

        @Serializable
        class ReferJoin(val joiner : PeerWithKey, val location : Location) : Message()

        @Serializable
        class AcceptJoin(val acceptor : PeerWithKey) : Message(), Initiate

        @Serializable
        class Join() : Message(), Initiate
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
 * and cannot be taken as indication that we have the sender's symkey
 */
interface Initiate

@Serializable
data class MessageId(val long : Long = random.nextLong())
