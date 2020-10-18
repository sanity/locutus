package locutus.tools.crypto.rsa

import kotlinx.serialization.Serializable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.interfaces.*

// TODO: Switch to using Elliptic Curve: https://docs.oracle.com/javacard/3.0.5/api/javacard/security/ECPrivateKey.html
//  https://docs.oracle.com/en/java/javase/13/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254
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