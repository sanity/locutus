@file:UseSerializers(RSAPublicKeySerializer::class)

package locutus.net.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kweb.util.random
import locutus.protocols.bw.BytesPerSecond
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
    class Keepalive : Message()

    object BW {
        @Serializable
        data class BWLimit(val bwLimit : BytesPerSecond) : Message(), CanInitiate {
            override val isInitiate = true
        }
    }

    object Ring {
        @Serializable
        data class JoinRequest(val type: Type, val hopsToLive: Int) : Message(), CanInitiate {
            override val isInitiate = false

            @Serializable
            sealed class Type {
                @Serializable
                data class Initial(val myPublicKey: RSAPublicKey) : Type()
                @Serializable
                data class Proxy(val joiner: PeerKeyLocation) : Type()
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
                @Serializable
                data class Initial(val yourExternalAddress: Peer, val yourLocation: Location) : Type()
                @Serializable
                object Proxy : Type() {
                    override fun toString() = "Proxy"
                }
            }
        }

        @Serializable
        data class OpenConnection(val myState : ConnectionState) : Message(), CanInitiate {
            override val isInitiate = myState == ConnectionState.Connecting

            @Serializable enum class ConnectionState {
                Connecting, OCReceived, Connected
            }
        }

        @Serializable
        data class CloseConnection(val reason: String) : Message()
    }

    object Probe {
        @Serializable
        data class ProbeRequest(val target : Location, val hopsToLive: Int) : Message()

        @Serializable
        data class ProbeResponse(val visits : List<Visit>, override val replyTo: MessageId) : Message(), Reply {
            @Serializable
            data class Visit(val hop : Int, val latency : Long, val location : Location)
        }
    }

    object Testing {
        @Serializable
        data class FooMessage(val v: Int, override val isInitiate: Boolean) : Message(), CanInitiate

        @Serializable
        data class BarMessage(val n: String) : Message()
    }
}

/**
 * Marker interface on a message which indicates it may initiate a connection,
 * and therefore can't be assumed to confirm the sender has our outbound symkey.
 *
 * [isInitiate] must also return true for this to be the case.
 */
interface CanInitiate {
    val isInitiate: Boolean
}

interface Reply {
    val replyTo: MessageId
}

@Serializable
data class MessageId(val long: Long = random.nextLong())
