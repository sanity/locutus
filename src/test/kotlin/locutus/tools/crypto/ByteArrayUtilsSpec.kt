package locutus.tools.crypto

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Order(0)
class ByteArrayUtilsSpec : FunSpec({
    test("ByteArray merge") {
        val arrays = listOf(byteArrayOf(1, 2), byteArrayOf(5, 8), byteArrayOf())
        arrays.merge() shouldBe byteArrayOf(1, 2, 5, 8)
    }

    test("ByteArray split") {
        val array = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val split = array.split(3)
        split.size shouldBe 3
        split[0] shouldBe byteArrayOf(1, 2, 3)
        split[1] shouldBe byteArrayOf(4, 5, 6)
        split[2] shouldBe byteArrayOf(7)
    }

})
