package locutus.net.messages

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.net.messages.Message.Ring.JoinResponse
import locutus.tools.crypto.rsa.RSAKeyPair
import locutus.tools.math.Location
import java.net.InetSocketAddress

class MessageSpec : FunSpec({
    context("Serialize/deserialize") {

        test("JoinResponse") {
            val peerKeyLocation = PeerKeyLocation(
                Peer(InetSocketAddress("localhost", 1234)),
                RSAKeyPair.create().public,
                Location(0.5)
            )
            val jr = JoinResponse(JoinResponse.Type.Proxy, setOf(), MessageId())
            val serialized = ProtoBuf.encodeToByteArray(JoinResponse.serializer(), jr)
            val jr2 = ProtoBuf.decodeFromByteArray(JoinResponse.serializer(), serialized)
            jr shouldBe jr2
        }
    }
})
