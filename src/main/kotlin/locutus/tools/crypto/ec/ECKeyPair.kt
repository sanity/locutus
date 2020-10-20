package locutus.tools.crypto.ec

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

data class ECKeyPair(val public: ECPublicKey, val private: ECPrivateKey) {
    companion object {

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun create() : ECKeyPair {
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
            val key = keyGen.generateKeyPair()
            return ECKeyPair(key.public as ECPublicKey, key.private as ECPrivateKey)
        }
    }
}