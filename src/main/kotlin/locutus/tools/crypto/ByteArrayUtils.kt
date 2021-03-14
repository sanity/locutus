package locutus.tools.crypto

import java.security.MessageDigest
import kotlin.experimental.and

fun ByteArray.hash(): ByteArray {
    val digest = MessageDigest.getInstance("SHA3-256")
    digest.reset()
    digest.digest(this)
    return digest.digest()
}

fun Iterable<ByteArray>.merge(): ByteArray {
    val sizes = this.map { it.size }
    val ttlSize = sizes.sum()
    val ret = ByteArray(ttlSize)
    var ix = 0
    for (ba in this) {
        ba.copyInto(ret, ix)
        ix += ba.size
    }
    return ret
}

fun ByteArray.split(maxPieceSize : Int) : ArrayList<ByteArray> {
    var pos = 0
    val ret = ArrayList<ByteArray>()
    while (pos < size) {
        val pieceEnd = pos + maxPieceSize
        ret += this.copyOfRange(pos, kotlin.math.min(pieceEnd, size))
        pos = pieceEnd
    }
    return ret
}

fun ByteArray.startsWith(prefix: ByteArray) : Boolean {
    for ((ix, b) in prefix.withIndex()) {
        if (this[ix] != b) return false
    }
    return true
}

fun Long.asByteArray(): ByteArray {
    var l = this
    val result = ByteArray(8)
    for (i in 7 downTo 0) {
        result[i] = (l and 0xFF).toByte()
        l = l shr 8
    }
    return result
}

fun ByteArray.asLong(): Long {
    var result: Long = 0
    for (i in 0 until java.lang.Long.BYTES) {
        result = result shl java.lang.Long.BYTES
        result = result or (this[i].toLong() and 0xFF)
    }
    return result
}