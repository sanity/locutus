package locutus.tools.crypto

import java.security.MessageDigest

object Util {
    fun hash(buffer: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.digest(buffer)
        return digest.digest()
    }


}

fun Iterable<ByteArray>.merge() : ByteArray {
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