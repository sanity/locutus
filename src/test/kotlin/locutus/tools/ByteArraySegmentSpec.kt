package locutus.tools

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.protobuf.ProtoBuf

class ByteArraySegmentSpec : FunSpec({
    test("asArray") {
        val bas = ByteArraySegment(byteArrayOf(1, 2, 3, 4), 1, 2)
        bas.asArray shouldBe byteArrayOf(2, 3)
    }

    test("serialization") {
        val bas = ByteArraySegment(byteArrayOf(1, 2, 3, 5, 8, 13))
        val serialized = ProtoBuf.dump(ByteArraySegment.serializer(), bas)
        val deserialized = ProtoBuf.load(ByteArraySegment.serializer(), serialized)
        deserialized shouldBe bas
    }
})
