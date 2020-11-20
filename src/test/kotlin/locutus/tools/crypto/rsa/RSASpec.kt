package locutus.tools.crypto.rsa

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import locutus.tools.crypto.AESKey
import java.nio.charset.Charset

@ExperimentalSerializationApi
@ExperimentalStdlibApi
@Order(0)
class RSASpec : FunSpec({
    test("simple encrypt decrypt") {
        val keyPair = RSAKeyPair.create()
        val plainText = "the rain in spain falls mainly on the plain".encodeToByteArray()
        val encoded = keyPair.public.encrypt(plainText)
        val decoded = keyPair.private.decrypt(encoded)
        decoded shouldBe plainText
    }

    test("ByteArray serialize") {
        @Serializable
        data class Foo(@ProtoNumber(1) val bar: ByteArray) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Foo

                if (!bar.contentEquals(other.bar)) return false

                return true
            }

            override fun hashCode(): Int {
                return bar.contentHashCode()
            }
        }

        val foo = Foo("ABCDEFG".toByteArray(Charset.forName("UTF-8")))

        val serialized = ProtoBuf.encodeToByteArray(Foo.serializer(), foo)

        serialized.size shouldBeLessThan 15

        val deserialized: Foo = ProtoBuf.decodeFromByteArray(Foo.serializer(), serialized)

        deserialized shouldBe foo
    }

    xtest("RSA AES encrypt-decrypt") {
        for (x in 0 .. 1000) {
            val keyPair = RSAKeyPair.create()
            val aesKey = AESKey.generate()
            val encrypted = keyPair.public.encrypt(aesKey.bytes).ciphertext
            val decrypted = keyPair.private.decrypt(RSAEncrypted(encrypted))
            decrypted shouldBe aesKey.bytes
        }
    }
})