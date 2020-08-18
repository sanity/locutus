package locutus.tools.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

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

        val rsa = RSAKeyPair.create()
        val pub = rsa.public.encrypt(key.bytes)
        pub.bytes.size shouldBe AESKey.RSA_ENCRYPTED_SIZE
    }
})

