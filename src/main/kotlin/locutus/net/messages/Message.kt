@file:UseSerializers(RSAPublicKeySerializer::class, DurationSerializer::class, IntRangeSerializer::class)

package locutus.net.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kweb.util.random
import locutus.protocols.probe.ProbeRequest
import locutus.protocols.probe.ProbeResponse
import locutus.protocols.ring.CloseConnection
import locutus.protocols.ring.JoinRequest
import locutus.protocols.ring.JoinResponse
import locutus.protocols.ring.OpenConnection
import locutus.protocols.ring.store.StorePut
import locutus.protocols.ring.store.StoreGet
import locutus.protocols.ring.store.StoreGetResponse
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
import locutus.tools.serializers.DurationSerializer
import locutus.tools.serializers.IntRangeSerializer
import java.time.Duration

typealias PartNo = Int
typealias Bytes = Int

@Serializable
abstract class Message {

    /**
     * Used for detecting duplicate messages
     */
    val id = MessageId()

}

val messageModule = SerializersModule {
    polymorphic(Message::class) {
        // Store
        subclass(StoreGet::class)
        subclass(StoreGetResponse::class)
        subclass(StorePut::class)

        // Ring
        subclass(JoinRequest::class)
        subclass(JoinResponse::class)
        subclass(OpenConnection::class)
        subclass(CloseConnection::class)

        // Probe
        subclass(ProbeRequest::class)
        subclass(ProbeResponse::class)

        // Misc
        subclass(Keepalive::class)
        subclass(RateLimit::class)

        // Testing
        subclass(FooMessage::class)
        subclass(BarMessage::class)
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
