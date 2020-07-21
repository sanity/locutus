package locutus.crypto

import java.security.MessageDigest

object TrUtil {
    fun hash(buffer: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.digest(buffer)
        return digest.digest()
    }

}