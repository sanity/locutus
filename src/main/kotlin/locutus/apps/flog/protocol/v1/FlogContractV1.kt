package locutus.apps.flog.protocol.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Value
import locutus.protocols.ring.contracts.ValueUpdate
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.verify
import java.security.interfaces.ECPublicKey

/*

What is a microblog?

An updatable value?



 */

/**
 * @param pubKey Valid posts must be signed by this public key
 * @param number The number of this post, starting with 0 and incrementing each time.
 *               If this is null then this points to the latest post, which will be
 *               updated with each new post.
 */
@Serializable
@SerialName("mb_v1")
data class FlogContractV1(val pubKey: ECPublicKey, val number: Int? = null) : Contract() {

    init {
        require(number == null || number >= 0)
    }

    override fun valid(retriever: GlobalStore, post: Value): Boolean {
        return if (post is FlogPostV1) {
            (number == null || number == post.payload.number) && pubKey.verify(post.signature, post.serializedPayload)
        } else {
            false
        }
    }

    override fun update(old: Value, update: ValueUpdate): FlogPostV1? {
        return when {
            old is FlogPostV1 && update is FlogUpdateV1 -> {
                return if (update.updated.payload.version > old.payload.version) {
                    update.updated
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }
    }
}