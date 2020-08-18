package locutus.tools.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.dump
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.nio.charset.Charset

@ExperimentalStdlibApi
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
        class Foo(@ProtoNumber(1) val bar: ByteArray)

        val foo = Foo("ABCDEFG".toByteArray(Charset.forName("UTF-8")))

        val serialized = ProtoBuf.encodeToByteArray(Foo.serializer(), foo)

        serialized.size shouldBeLessThan 15

        val deserialized: Foo = ProtoBuf.decodeFromByteArray(Foo.serializer(), serialized)

        deserialized shouldBe foo
    }
})