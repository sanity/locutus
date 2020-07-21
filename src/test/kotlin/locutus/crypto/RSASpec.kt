package locutus.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoId
import locutus.tools.ByteArraySegment
import java.nio.charset.Charset

@ExperimentalStdlibApi
class RSASpec : FunSpec({
    test("simple encrypt decrypt") {
        val keyPair = RSAKeyPair.create()
        val plainText = ByteArraySegment("the rain in spain falls mainly on the plain".encodeToByteArray())
        val encoded = keyPair.public.encrypt(plainText)
        val decoded = keyPair.private.decrypt(encoded)
        decoded shouldBe plainText
    }

    test("ByteArray serialize") {
        @Serializable
        data class Foo(@ProtoId(1) val bar: ByteArraySegment)

        val foo = Foo(ByteArraySegment("ABCDEFG".toByteArray(Charset.forName("UTF-8"))))

        val serialized = ProtoBuf.dump(Foo.serializer(), foo)

        serialized.size shouldBeExactly 15

        val deserialized: Foo = ProtoBuf.load(Foo.serializer(), serialized)

        deserialized shouldBe foo
    }
})