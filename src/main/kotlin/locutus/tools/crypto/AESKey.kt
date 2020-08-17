package locutus.tools.crypto

import locutus.tools.ByteArraySegment
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESKey(private val byteArraySegment: ByteArraySegment) {

    companion object {
        private const val CIPHER_NAME = "AES/CBC/PKCS7Padding"

        const val KEY_SIZE_BYTES = 16
        const val RSA_ENCRYPTED_SIZE = 256

        var overhead: Int

        private val rng = SecureRandom()

        init {
            Security.addProvider(BouncyCastleProvider())
            try {
                overhead = Cipher.getInstance(CIPHER_NAME).blockSize
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        fun generate(): AESKey {
            val kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            val skey = kgen.generateKey();
            return AESKey(ByteArraySegment(skey.encoded));
        }
    }


    private val skey: SecretKeySpec =
        SecretKeySpec(byteArraySegment.array, byteArraySegment.offset, byteArraySegment.length, "AES")
    val bytes: ByteArray by lazy {
        skey.encoded
    }

    fun decrypt(toDecrypt: ByteArraySegment): ByteArraySegment {
        return try {
            val cipher = Cipher.getInstance(CIPHER_NAME)
            val ivSpec =
                IvParameterSpec(toDecrypt.array, toDecrypt.offset, overhead)
            cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec)
            ByteArraySegment(
                cipher.doFinal(
                    toDecrypt.array,
                    toDecrypt.offset + overhead,
                    toDecrypt.length - overhead
                )
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun encrypt(toEncrypt: ByteArraySegment): ByteArraySegment {
        return try {
            val iv = ByteArray(overhead)
            synchronized(rng) { rng.nextBytes(iv) }
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.ENCRYPT_MODE, skey, IvParameterSpec(iv))
            val ciphertext = cipher.doFinal(toEncrypt.array, toEncrypt.offset, toEncrypt.length)
            ByteArraySegment(iv + ciphertext)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun asByteArraySegment(): ByteArraySegment {
        return ByteArraySegment(bytes)
    }

}
