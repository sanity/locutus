package locutus.tools.crypto.rsa

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import java.security.interfaces.RSAPublicKey

private val logger = KotlinLogging.logger {}

@ExperimentalSerializationApi
class RSAPublicKeySerializerSpec : FunSpec({
    test("serialization") {
        val keyPair = RSAKeyPair.create()
        val serializedPubKey = ProtoBuf.encodeToByteArray(RSAPublicKeySerializer, keyPair.public)
        logger.info("Serialized size: ${serializedPubKey.size}")
        val deserializedPubKey = ProtoBuf.decodeFromByteArray(RSAPublicKeySerializer, serializedPubKey)
        deserializedPubKey shouldBe keyPair.public
    }
})
