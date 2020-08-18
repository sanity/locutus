package locutus.tools.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import locutus.tools.ByteArraySegment
import locutus.tools.crypto.Util.merge

class UtilSpec : FunSpec({
    test("ByteArray merge") {
        val arrays = listOf(byteArrayOf(1, 2), byteArrayOf(5, 8), byteArrayOf()).map { ByteArraySegment(it) }
        arrays.merge() shouldBe byteArrayOf(1, 2, 5, 8)
    }
})
