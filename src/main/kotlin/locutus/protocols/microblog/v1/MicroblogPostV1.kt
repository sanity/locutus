package locutus.protocols.microblog.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.contracts.Post
import locutus.tools.crypto.ec.ECSignature

@Serializable @SerialName("mb_post_v1")
class MicroblogPostV1(val signature: ECSignature, val serializedPayload: ByteArray) : Post() {
    val payload: MicroblogPayloadV1 by lazy { ProtoBuf.decodeFromByteArray(MicroblogPayloadV1.serializer(), serializedPayload) }
}