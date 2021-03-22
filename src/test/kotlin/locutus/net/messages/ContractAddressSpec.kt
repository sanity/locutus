package locutus.net.messages

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.protocols.ring.contracts.ContractAddress
import locutus.tools.crypto.hash

@ExperimentalSerializationApi
@Order(0)
class ContractAddressSpec : FunSpec({
    test("Serialize and deserialize") {
        val original = ContractAddress(byteArrayOf(1, 2,3, 4).hash())
        val encoded = original.asBase58Encoded
        val decoded = ContractAddress.fromBase58Encoded(encoded)
        decoded shouldBe original
    }
})
