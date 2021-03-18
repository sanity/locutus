package locutus.net.messages

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.contracts.Address
import locutus.tools.crypto.hash
import java.net.InetSocketAddress

@ExperimentalSerializationApi
@Order(0)
class AddressSpec : FunSpec({
    test("Serialize and deserialize") {
        val original = Address(byteArrayOf(1, 2,3, 4).hash())
        val encoded = original.asBase58Encoded
        val decoded = Address.fromBase58Encoded(encoded)
        decoded shouldBe original
    }
})
