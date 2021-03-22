package locutus.tools

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PartTrackerSpec : FunSpec({
    test("No parts received") {
        val partTracker = PartTracker()
        partTracker.missingRanges(5) shouldBe arrayListOf(0 .. 4)
    }
    test("First part received") {
        val partTracker = PartTracker()
        partTracker += 0
        partTracker.missingRanges(5) shouldBe arrayListOf(1 .. 4)
    }
    test("First part received twice") {
        val partTracker = PartTracker()
        partTracker += 0
        partTracker += 0
        partTracker.missingRanges(5) shouldBe arrayListOf(1 .. 4)
    }
    test("First and second parts received") {
        val partTracker = PartTracker()
        partTracker += 0
        partTracker += 1
        partTracker.missingRanges(5) shouldBe arrayListOf(2 .. 4)
    }
    test("First and third parts received") {
        val partTracker = PartTracker()
        partTracker += 0
        partTracker += 2
        partTracker.missingRanges(5) shouldBe arrayListOf(1 .. 1, 3 .. 4)
    }
    test("Second and last parts received") {
        val partTracker = PartTracker()
        partTracker += 1
        partTracker += 4
        partTracker.missingRanges(5) shouldBe arrayListOf(0 .. 0, 2 .. 3)
    }
    test("Second and third then first parts received") {
        val partTracker = PartTracker()
        partTracker += 1
        partTracker += 2
        partTracker += 0
        partTracker.missingRanges(5) shouldBe arrayListOf(3 .. 4)
    }
})
