package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.ECSignature
import locutus.tools.crypto.ec.verify
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

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
data class MicroblogContractV1(val pubKey: ECPublicKey, val number: Int? = null) : Contract() {

    init {
        require(number == null || number >= 0)
    }

    override fun valid(retriever: GlobalStore, post: Post): Boolean {
        return if (post is MicroblogPostV1) {
            (number == null || number == post.payload.number) && pubKey.verify(post.signature, post.serializedPayload)
        } else {
            false
        }
    }

    override fun supersedes(old: Post, new: Post): Boolean {
        return if (old is MicroblogPostV1 && new is MicroblogPostV1) {
            new.payload.version > old.payload.version
        } else {
            throw ClassCastException("Post $old or $new isn't a MicroblogPost")
        }
    }

}

@Serializable
class MicroblogPayloadV1(val number: Int, val version: Int, val serializedMessage: ByteArray) {
    val message: MicroblogMessage by lazy { ProtoBuf.decodeFromByteArray(MicroblogMessage.serializer(), serializedMessage) }
}

@Serializable
sealed class MicroblogMessage {
    @Serializable
    data class Text(val text: String) : MicroblogMessage()
}


@Serializable
class MicroblogPostV1(val signature: ECSignature, val serializedPayload: ByteArray) : Post() {
    val payload: MicroblogPayloadV1 by lazy { ProtoBuf.decodeFromByteArray(MicroblogPayloadV1.serializer(), serializedPayload) }
}