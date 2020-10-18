package locutus.tools.crypto.rsa

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExperimentalSerializationApi
@Order(0)
class RSAPublicKeySerializerSpec : FunSpec({
    test("serialization") {
        val keyPair = ECKeyPair.create()
        val serializedPubKey = ProtoBuf.encodeToByteArray(ECPublicKeySerializer, keyPair.public)
        logger.info("Serialized size: ${serializedPubKey.size}")
        val deserializedPubKey = ProtoBuf.decodeFromByteArray(ECPublicKeySerializer, serializedPubKey)
        deserializedPubKey shouldBe keyPair.public
    }
})
