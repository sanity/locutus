package locutus.tools.crypto

import locutus.tools.ByteArraySegment
import java.security.MessageDigest

object Util {
    fun hash(buffer: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.digest(buffer)
        return digest.digest()
    }


}

fun Iterable<ByteArraySegment>.merge() : ByteArray {
    val sizes = this.map { it.size }
    val ttlSize = sizes.sum()
    val ret = ByteArray(ttlSize)
    var ix = 0
    for (ba in this) {
        ba.array.copyInto(ret, ix, ba.offset, ba.offset + ba.length)
        ix += ba.size
    }
    return ret
}