package locutus.tools.crypto

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import locutus.tools.crypto.rsa.*

@Order(0)
class AESKeySpec : FunSpec({
    test("Create, encrypt and decrypt") {
        val key = AESKey.generate()
        val origMessage = "secretMessage".toByteArray()
        val encrypted = key.encrypt(origMessage)
        val decrypted = key.decrypt(encrypted)
        decrypted shouldBe origMessage
    }

    test("Verify size") {
        val key = AESKey.generate()
        key.bytes.size shouldBe AESKey.KEY_SIZE_BYTES

        val rsa = ECKeyPair.create()
        val pub = rsa.public.encrypt(key.bytes)
        pub.ciphertext.size shouldBe AESKey.RSA_ENCRYPTED_SIZE
    }
})

