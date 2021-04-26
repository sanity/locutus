package locutus.protocols.ring

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.net.messages.*
import locutus.tools.math.Location
import java.security.interfaces.RSAPublicKey

@Serializable
@SerialName("joinRequest")
data class JoinRequest(val type: Type, val hopsToLive: Int) : Message(), CanInitiate {
    override val isInitiate = false

    @Serializable
    sealed class Type {
        @Serializable
        @SerialName("initial")
        data class Initial(val myPublicKey: RSAPublicKey) : Type()

        @Serializable
        @SerialName("proxy")
        data class Proxy(val joiner: PeerKeyLocation) : Type()
    }
}

@Serializable
@SerialName("joinResponse")
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
        @SerialName("initial")
        data class Initial(val yourExternalAddress: Peer, val yourLocation: Location) : Type()

        @Serializable
        @SerialName("proxy")
        object Proxy : Type() {
            override fun toString() = "Proxy"
        }
    }
}

@Serializable
@SerialName("openConnection")
data class OpenConnection(val myState: ConnectionState) : Message(), CanInitiate {
    override val isInitiate = myState == ConnectionState.Connecting

    @Serializable
    enum class ConnectionState {
        Connecting, OCReceived, Connected
    }
}

@Serializable
@SerialName("closeConnection")
data class CloseConnection(val reason: String) : Message()