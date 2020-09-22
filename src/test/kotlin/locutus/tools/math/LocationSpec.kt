package locutus.tools.math

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.*
import io.kotest.matchers.shouldBe
import kweb.util.random
import mu.KotlinLogging

private val tolerance = 0.000000001

private val logger = KotlinLogging.logger {}

@ExperimentalUnsignedTypes
class LocationSpec : FunSpec({
    test("Distance") {
        (Location(0.4) distance Location(0.6)) shouldBe (0.2 plusOrMinus tolerance)
        (Location(0.1) distance Location(0.9)) shouldBe (0.2 plusOrMinus tolerance)
    }

    test("fromByteArray simple") {
        Location.fromByteArray(ubyteArrayOf(UByte.MIN_VALUE)).value shouldBe (0.0 plusOrMinus tolerance)
        Location.fromByteArray(ubyteArrayOf(UByte.MAX_VALUE)).value shouldBe (1.0 plusOrMinus 0.01)
        Location.fromByteArray(ubyteArrayOf(UByte.MAX_VALUE, UByte.MAX_VALUE)).value shouldBe (1.0 plusOrMinus 0.0001)
        Location.fromByteArray(ubyteArrayOf(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE)).value shouldBe (1.0 plusOrMinus 0.000001)
        Location.fromByteArray(ubyteArrayOf(UByte.MIN_VALUE, UByte.MAX_VALUE)).value shouldBe ((1.0/256.0) plusOrMinus 0.001)
    }

    test("fromByteArray precision") {
        val randomBytes = ByteArray(20)
        random.nextBytes(randomBytes)

        val randomUBytes = randomBytes.asUByteArray()

        for (precision in 1 .. 7) {
            val pos = Location.fromByteArray(randomUBytes, precision)
            logger.info("precision: $precision  position: $pos")
        }
    }
})
