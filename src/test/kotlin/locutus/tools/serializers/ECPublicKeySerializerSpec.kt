package locutus.tools.serializers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.tools.crypto.ec.ECKeyPair

class ECPublicKeySerializerSpec : FunSpec({
    test("Simple serialize/deserialize") {
        val pair = ECKeyPair.create()
        val serializedPubKey = ProtoBuf.encodeToByteArray(ECPublicKeySerializer, pair.public)
        println("Serialized EC public key size: ${serializedPubKey.size} bytes")
        val deserializedPubKey = ProtoBuf.decodeFromByteArray(ECPublicKeySerializer, serializedPubKey)
        deserializedPubKey shouldBe pair.public
    }
})
