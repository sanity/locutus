package locutus.protocols.microblog.v1

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable
class MicroblogPayloadV1(val number: Int, val version: Int, val serializedMessage: ByteArray) {
    val message: MicroblogMessageV1 by lazy { ProtoBuf.decodeFromByteArray(MicroblogMessageV1.serializer(), serializedMessage) }
}