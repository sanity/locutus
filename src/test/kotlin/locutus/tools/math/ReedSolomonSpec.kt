package locutus.tools.math

import com.backblaze.erasure.CodingLoop
import com.backblaze.erasure.ReedSolomon
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kweb.util.random

class ReedSolomonSpec : FunSpec({
    test("Simple encode and decode") {
        val rs = ReedSolomon.create(2, 2)
        val shard1 = ByteArray(10)
        random.nextBytes(shard1)
        val shard2 = ByteArray(10)
        random.nextBytes(shard2)
        val shard3 = ByteArray(10)
        val shard4 = ByteArray(10)
        val shards = arrayOf(shard1, shard2, shard3, shard4)
        rs.encodeParity(shards, 0, 10)

        val missingShards = arrayOf(ByteArray(10), shard2, shard3, ByteArray(10))
        rs.decodeMissing(missingShards, booleanArrayOf(false, true, true, false), 0, 10)

        missingShards[0].contentEquals(shard1) shouldBe true
        missingShards[3].contentEquals(shard4) shouldBe true
    }
})