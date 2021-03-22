package locutus.tools.math.fec

import io.kotest.core.spec.style.FunSpec
import kweb.util.random
import locutus.tools.math.fec.util.Buffer

class PureCodeSpec : FunSpec({
    test("simple") {
        val pureCode = PureCode(2, 4)
        val toEncode = ByteArray(20)
        random.nextBytes(toEncode)
        println("toEncode: ${toEncode.map { it.toInt() }}")
        val b1 = Buffer(toEncode, 0, 10)
        val b2 = Buffer(toEncode, 10, 10)
        val encoded = arrayOf<Buffer>(
            Buffer(ByteArray(10)),
            Buffer(ByteArray(10)),
            Buffer(ByteArray(10)),
            Buffer(ByteArray(10))
        )
        pureCode.encode(
            arrayOf(b1, b2),
            encoded,
            intArrayOf(0, 1, 2, 3)
        )
        for (x in encoded.indices) {
            println("$x\t${encoded[x].bytes.map { it.toInt() }}")
        }

        val received = arrayOf<Buffer>(Buffer(ByteArray(10)),
                                encoded[1], encoded[2], Buffer(ByteArray(10)))

        println("Pre-decode")
        for (x in received.indices) {
            println("$x\t${received[x].bytes.map {it.toInt()}}")
        }

        pureCode.decode(received, intArrayOf(0, 1, 2, 3))

        println("Decoded")
        for (x in received.indices) {
            println("$x\t${received[x].bytes.map {it.toInt()}}")
        }
    }
})
