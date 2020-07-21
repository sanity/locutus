package locutus.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import locutus.tools.toImmutable

class TrSymKeySpec : FunSpec({
    test("Create, encrypt and decrypt") {
        val key = AESKey.generate()
        val origMessage = "secretMessage".toByteArray().toImmutable()
        val encrypted = key.encrypt(origMessage)
        val decrypted = key.decrypt(encrypted)
        decrypted shouldBe origMessage
    }
})
