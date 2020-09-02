package locutus.net.messages

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.net.InetSocketAddress

@ExperimentalSerializationApi
class NodeAddressSpec : FunSpec({
    test("Serialize and deserialize") {
        val inetSocketAddress = InetSocketAddress("localhost", 1234)
        val nodeAddress = Peer(inetSocketAddress)
        val serialized = ProtoBuf.encodeToByteArray(Peer.serializer(), nodeAddress)
        val deserialized = ProtoBuf.decodeFromByteArray(Peer.serializer(), serialized)
        deserialized shouldBe nodeAddress
        deserialized.asSocketAddress shouldBe inetSocketAddress
    }
})
