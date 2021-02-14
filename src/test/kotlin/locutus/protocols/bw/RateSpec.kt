package locutus.protocols.bw

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.time.Instant

class RateSpec : FunSpec({
    test("Verify Rate") {
        val rate = Rate()
        val startTime = Instant.now()
        rate.registerTraffic(10, startTime)
        val secondTime = startTime.plusSeconds(30)
        rate.registerTraffic(20, secondTime)

        rate.rate(secondTime) shouldBe (0.5 plusOrMinus 0.0001)

        val thirdTime = secondTime.plusSeconds(40)
        rate.registerTraffic(30, thirdTime)

        rate.rate(thirdTime)
    }
})
