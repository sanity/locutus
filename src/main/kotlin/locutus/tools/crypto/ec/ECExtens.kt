package locutus.tools.crypto.ec

import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey


fun ECPrivateKey.sign(toSign : ByteArray) : ECSignature {
    val sig : Signature = Signature.getInstance("SHA256withECDSA", "BC")
    sig.initSign(this)
    sig.update(toSign)
    return ECSignature(sig.sign())
}

fun ECPublicKey.verify(signature : ECSignature, toVerify : ByteArray) : Boolean {
    val sig = Signature.getInstance("SHA256withECDSA", "BC")
    sig.initVerify(this)
    sig.update(toVerify)
    return sig.verify(signature.array)
}

