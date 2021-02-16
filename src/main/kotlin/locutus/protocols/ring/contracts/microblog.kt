package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.RSAPublicKey
import kotlin.reflect.KClass

@Serializable
data class MicroblogContract(val pubKey : RSAPublicKey, val number : Int? = null) : Contract() {
    override val postType: KClass<out Post>
        get() = TODO("Not yet implemented")

    override fun valid(p: Post): Boolean {
        return if (p is MicroblogPost) {
            (number == null || number == p.number) && pubKey.verify(p.signature, p.payload)
        } else {
            false
        }
    }

    override fun supersedes(old: Post, new: Post): Boolean {
        return if (old is MicroblogPost && new is MicroblogPost) {
            return new.number > old.number
        } else {
            error("old (${old::class.qualifiedName}) and new (${new::class.qualifiedName}) aren't MicroblogPosts")
        }
    }

}



@Serializable
class MicroblogPost(val signature : RSASignature, val number : Int, val payload : ByteArray) : Post()