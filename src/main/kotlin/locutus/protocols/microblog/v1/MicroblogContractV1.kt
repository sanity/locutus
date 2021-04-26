package locutus.protocols.microblog.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Post
import locutus.protocols.ring.contracts.PostUpdate
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.verify
import java.security.interfaces.ECPublicKey

/**
 * @param pubKey Valid posts must be signed by this public key
 * @param number The number of this post, starting with 0 and incrementing each time.
 *               If this is null then this points to the latest post, which will be
 *               updated with each new post.
 */
@Serializable
@SerialName("mb_v1")
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

    override fun update(old: Post, update: PostUpdate): Post? {
        return if (old is MicroblogPostV1 && update is MicroblogUpdateV1) {
            if (update.updated.payload.version > old.payload.version) {
                update.updated
            } else {
                null
            }
        } else {
            throw ClassCastException("Post $old or $update isn't a MicroblogPost")
        }
    }

}