package locutus.protocols.ring.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.ec.ECSignature

@Serializable
sealed class Post()

@Serializable @SerialName("lfp_v1")
class LargeFilePostV1(val signature : ECSignature, val serializedPart : ByteArray) : Post() {

}

@Serializable @SerialName("mb_post_v1")
class MicroblogPostV1(val signature: ECSignature, val serializedPayload: ByteArray) : Post() {
    val payload: MicroblogPayloadV1 by lazy { ProtoBuf.decodeFromByteArray(MicroblogPayloadV1.serializer(), serializedPayload) }
}