@file:UseSerializers(RSAPublicKeySerializer::class)

package locutus.net.messages

import kotlinx.serialization.*
import kweb.util.random
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
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
        data class JoinResponse(
            val type: Type,
            // Note: ProtoBuf requires emptySet() default value, without it we get a runtime MissingFieldException at
            // deserialization, see: https://github.com/Kotlin/kotlinx.serialization/issues/806
            val acceptedBy: Set<PeerKeyLocation> = emptySet(),
            override val replyTo: MessageId
        ) : Message(), Reply {
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
