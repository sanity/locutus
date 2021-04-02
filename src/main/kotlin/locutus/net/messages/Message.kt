@file:UseSerializers(RSAPublicKeySerializer::class, DurationSerializer::class, IntRangeSerializer::class)

package locutus.net.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kweb.util.random
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.ContractAddress
import locutus.protocols.ring.contracts.Post
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
import locutus.tools.math.Location
import locutus.tools.serializers.DurationSerializer
import locutus.tools.serializers.IntRangeSerializer
import java.security.interfaces.RSAPublicKey
import java.time.Duration

typealias PartNo = Int
typealias Bytes = Int

@Serializable
sealed class Message {

    /**
     * Used for detecting duplicate messages
     */
    val id = MessageId()

    @Serializable
    class Keepalive : Message()

    object Meta {
        @Serializable @SerialName("rateLimit")
        data class RateLimit(val minimumMessageInterval: Duration) : Message(), CanInitiate {
            override val isInitiate = true
        }

        @Serializable @SerialName("largeMsg")
        class LargeMessage(
            val uid: Int,
            val totalSize: Bytes,
            val partNo: Int,
            val totalParts: Int,
            val payload: ByteArray,
            val expectNextMessageBy: Duration?,
            override val isInitiate: Boolean
        ) : Message(), CanInitiate

        @Serializable @SerialName("largeMsgResend")
        class LargeMessageResend(val uid: Int, val missingParts: List<IntRange>, val lastReceivedPartNo: PartNo?) : Message()

    }

    object Store {
        @Serializable @SerialName("activeSub")
        data class ActiveSubscriptions(val contractAddresses: Set<ContractAddress>) : Message()

        @Serializable @SerialName("storeGet")
        data class Get(val contractAddress: ContractAddress, val requestId : Int, val sendContract : Boolean, val sendPost : Boolean, val hopsToLive: Int, val subscribe : Boolean) : Message()

        @Serializable @SerialName("storeResponse")
        data class Response(val requestId : Int, val type : Type) : Message() {
            @Serializable
            sealed class Type {
                @Serializable @SerialName("s")
                data class Success(val contract : Contract?, val post : Post?) : Type()

                @Serializable @SerialName("f")
                data class Failure(val reason : String) : Type()

            }
        }
    }

    object Ring {
        @Serializable @SerialName("joinRequest")
        data class JoinRequest(val type: Type, val hopsToLive: Int) : Message(), CanInitiate {
            override val isInitiate = false

            @Serializable
            sealed class Type {
                @Serializable @SerialName("initial")
                data class Initial(val myPublicKey: RSAPublicKey) : Type()

                @Serializable @SerialName("proxy")
                data class Proxy(val joiner: PeerKeyLocation) : Type()
            }
        }

        @Serializable @SerialName("joinResponse")
        data class JoinResponse(
            val type: Type,
            // Note: ProtoBuf requires emptySet() default value, without it we get a runtime MissingFieldException at
            // deserialization, see: https://github.com/Kotlin/kotlinx.serialization/issues/806
            val acceptedBy: Set<PeerKeyLocation> = emptySet(),
            override val replyTo: MessageId
        ) : Message(), Reply {
            @Serializable
            sealed class Type {
                @Serializable @SerialName("initial")
                data class Initial(val yourExternalAddress: Peer, val yourLocation: Location) : Type()

                @Serializable @SerialName("proxy")
                object Proxy : Type() {
                    override fun toString() = "Proxy"
                }
            }
        }

        @Serializable @SerialName("openConnection")
        data class OpenConnection(val myState: ConnectionState) : Message(), CanInitiate {
            override val isInitiate = myState == ConnectionState.Connecting

            @Serializable
            enum class ConnectionState {
                Connecting, OCReceived, Connected
            }
        }

        @Serializable @SerialName("closeConnection")
        data class CloseConnection(val reason: String) : Message()
    }

    object Probe {
        @Serializable @SerialName("probeRequest")
        data class ProbeRequest(val target: Location, val hopsToLive: Int) : Message()

        @Serializable @SerialName("probeResponse")
        data class ProbeResponse(val visits: List<Visit>, override val replyTo: MessageId) : Message(), Reply {
            @Serializable
            data class Visit(val hop: Int, val latency: Long, val location: Location)
        }
    }

    object Testing {
        @Serializable @SerialName("foo")
        data class FooMessage(val v: Int, override val isInitiate: Boolean) : Message(), CanInitiate

        @Serializable @SerialName("bar")
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
