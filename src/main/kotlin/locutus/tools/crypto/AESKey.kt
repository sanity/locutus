package locutus.tools.crypto

import kotlinx.serialization.Serializable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable class AESKey(val bytes: ByteArray) {

    companion object {
        private const val CIPHER_NAME = "AES/CBC/PKCS7Padding"

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        const val KEY_SIZE_BYTES = 16
        const val RSA_ENCRYPTED_SIZE = 256

        val blockSize: Int = run {
            Cipher.getInstance(CIPHER_NAME).blockSize
        }

        private val rng = SecureRandom()


        fun generate(): AESKey {
            val kgen = KeyGenerator.getInstance("AES")
            kgen.init(128)
            val skey = kgen.generateKey()
            return AESKey(skey.encoded)
        }
    }


    private val toSecretKeySpec: SecretKeySpec by lazy { SecretKeySpec(bytes, "AES") }

    fun decrypt(toDecrypt: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance(CIPHER_NAME)
            val ivSpec =
                IvParameterSpec(toDecrypt, 0, blockSize)
            cipher.init(Cipher.DECRYPT_MODE, toSecretKeySpec, ivSpec)
            cipher.doFinal(
                toDecrypt,
                blockSize,
                toDecrypt.size - blockSize
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun encrypt(toEncrypt: ByteArray): ByteArray {
        return try {
            val iv = ByteArray(blockSize)
            synchronized(rng) { rng.nextBytes(iv) }
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.ENCRYPT_MODE, toSecretKeySpec, IvParameterSpec(iv))
            val ciphertext = cipher.doFinal(toEncrypt)
            listOf(iv, ciphertext).merge()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
