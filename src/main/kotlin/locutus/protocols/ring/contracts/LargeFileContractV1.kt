package locutus.protocols.ring.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.ECSignature
import locutus.tools.crypto.ec.verify
import java.security.interfaces.ECPublicKey

////////////////////////////////////////////////////////
// Large File Contract
///////////////////////////////////////////////////////

typealias Bytes = Long

@Serializable @SerialName("lf_v1")
class LargeFileContractV1(val pubKey : ECPublicKey, val part : Int, val parts : Int, val parityParts : Int, val blockSize : Bytes) : Contract() {
    override fun valid(retriever: GlobalStore, p: Post): Boolean {
        return if (p is LargeFilePostV1) {
            pubKey.verify(p.signature, p.serializedPart)
        } else {
            false
        }
    }

    override fun supersedes(old: Post, new: Post): Boolean {
        TODO("Not yet implemented")
    }

    @Serializable @SerialName("lfp_v1")
    class LargeFilePostV1(val signature : ECSignature, val serializedPart : ByteArray) : Post() {

    }

}