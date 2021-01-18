package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.rsa.RSASignature

sealed class Post {
    abstract val id : Int
}

@Serializable
class SignedPost(
    val signature: RSASignature,
    val payload: ByteArray,
    override val id: Int
) : Post() {
    val message : Message get() = ProtoBuf.decodeFromByteArray(Message.serializer(), payload)
}