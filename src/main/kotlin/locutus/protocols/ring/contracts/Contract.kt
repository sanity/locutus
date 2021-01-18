package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.RSAPublicKey
import kotlin.reflect.KClass

@Serializable
sealed class Contract {
    abstract val postType : KClass<out Post>

    abstract fun valid(p : Post) : Boolean
}

@Serializable
data class SignedPostContract(val pubKey : RSAPublicKey) : Contract() {
    override val postType = SignedPost::class

    override fun valid(p: Post): Boolean {
        if (p !is SignedPost) return false
        return pubKey.verify(p.signature, p.payload)
    }
}

