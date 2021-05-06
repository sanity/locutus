package locutus.apps.flog.protocol.v1

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable
class FlogPayloadV1(val number: Int, val version: Int, val serializedMessage: ByteArray) {
    val message: FlogMessageV1 by lazy { ProtoBuf.decodeFromByteArray(FlogMessageV1.serializer(), serializedMessage) }
}