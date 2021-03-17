package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.RSAPublicKey
import kotlin.reflect.KClass

@Serializable
sealed class Contract {
    abstract fun valid(retriever: ContractRetriever, p: Post): Boolean

    abstract fun supersedes(old: Post, new: Post): Boolean
}

interface ContractRetriever {
    fun retrieve(contract: Contract): Post
}

@Serializable
sealed class Post() {

}

////////////////////////////////////////////////////////
// Utilities
////////////////////////////////////////////////////////

abstract class ContractView : Contract() {
    abstract fun keyExtractor(post : Post) : Any
}

////////////////////////////////////////////////////////
// Microblog
////////////////////////////////////////////////////////

/**
 * @param pubKey Valid posts must be signed by this public key
 * @param number The number of this post, starting with 0 and incrementing each time.
 *               If this is null then this points to the latest post, which will be
 *               updated with each new post.
 */
@Serializable
data class MicroblogContract(val pubKey: RSAPublicKey, val number: Int? = null) : Contract() {

    init {
        require(number == null || number >= 0)
    }

    override fun valid(retriever: ContractRetriever, post: Post): Boolean {
        return if (post is MicroblogPost) {
            (number == null || number == post.payload.number) && pubKey.verify(post.signature, post.serializedPayload)
        } else {
            false
        }
    }

    override fun supersedes(old: Post, new: Post): Boolean {
        return if (old is MicroblogPost && new is MicroblogPost) {
            new.payload.version > old.payload.version
        } else {
            throw ClassCastException("Post $old or $new isn't a MicroblogPost")
        }
    }

}

@Serializable
class MicroblogPayload(val number: Int, val version: Int, val serializedMessage: ByteArray) {
    val message: MicroblogMessage by lazy { ProtoBuf.decodeFromByteArray(MicroblogMessage.serializer(), serializedMessage) }
}

@Serializable
sealed class MicroblogMessage {
    @Serializable
    data class Text(val text: String) : MicroblogMessage()
}


@Serializable
class MicroblogPost(val signature: RSASignature, val serializedPayload: ByteArray) : Post() {
    val payload: MicroblogPayload by lazy { ProtoBuf.decodeFromByteArray(MicroblogPayload.serializer(), serializedPayload) }
}