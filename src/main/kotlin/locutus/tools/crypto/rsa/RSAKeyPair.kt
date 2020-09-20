package locutus.tools.crypto.rsa

import kotlinx.serialization.Serializable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.interfaces.*

data class RSAKeyPair(val public : RSAPublicKey, val private : RSAPrivateKey) {
    companion object {

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun create() : RSAKeyPair {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val key = keyGen.generateKeyPair()
            return RSAKeyPair(key.public as RSAPublicKey, key.private as RSAPrivateKey)
        }
    }
}