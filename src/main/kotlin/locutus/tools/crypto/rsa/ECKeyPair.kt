package locutus.tools.crypto.rsa

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.interfaces.*

// TODO: Switch to using Elliptic Curve: https://docs.oracle.com/javacard/3.0.5/api/javacard/security/ECPrivateKey.html
//  https://docs.oracle.com/en/java/javase/13/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254
data class ECKeyPair(val public : ECPublicKey, val private : ECPrivateKey) {
    companion object {

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun create() : ECKeyPair {
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(256)
            val key = keyGen.generateKeyPair()
            return ECKeyPair(key.public as ECPublicKey, key.private as ECPrivateKey)
        }
    }
}