package locutus.apps.flog.protocol.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.contracts.Value
import locutus.tools.crypto.ec.ECSignature

@Serializable @SerialName("mb_post_v1")
class FlogPostV1(val signature: ECSignature, val serializedPayload: ByteArray) : Value() {
    val payload: FlogPayloadV1 by lazy { ProtoBuf.decodeFromByteArray(FlogPayloadV1.serializer(), serializedPayload) }
}