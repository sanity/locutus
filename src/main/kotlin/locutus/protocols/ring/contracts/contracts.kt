package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.RSAPublicKey
import kotlin.reflect.KClass

@Serializable
sealed class Contract {
    abstract val postType : KClass<out Post>

    abstract fun valid(p : Post) : Boolean

    abstract fun supersedes(old : Post, new : Post) : Boolean
}

sealed class Post {

}

/**
 * @param pubKey Valid posts must be signed by this public key
 * @param number The number of this post, starting with 0 and incrementing each time.
 *               If this is null then this points to the latest post, which will be
 *               updated with each new post.
 */
@Serializable
data class MicroblogContract(val pubKey : RSAPublicKey, val number : Int? = null) : Contract() {
    override val postType: KClass<out Post>
        get() = TODO("Not yet implemented")

    override fun valid(p: Post): Boolean {
        return if (p is MicroblogPost) {
            (number == null || number == p.payload.number) && pubKey.verify(p.signature, p.serializedPayload)
        } else {
            false
        }
    }

    override fun supersedes(old: Post, new: Post): Boolean {
        return if (old is MicroblogPost && new is MicroblogPost) {
            return new.payload.version > old.payload.version
        } else {
            error("old (${old::class.qualifiedName}) and new (${new::class.qualifiedName}) aren't MicroblogPosts")
        }
    }

}

@Serializable
class MicroblogPost(val signature : RSASignature, val serializedPayload : ByteArray) : Post() {
    val payload : MicroblogPayload by lazy { ProtoBuf.decodeFromByteArray(MicroblogPayload.serializer(), serializedPayload) }
}

@Serializable
class MicroblogPayload(val number : Int, val version : Int, val serializedMessage : ByteArray)