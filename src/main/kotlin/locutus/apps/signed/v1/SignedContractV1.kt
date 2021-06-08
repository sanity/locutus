package locutus.apps.signed.v1

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Value
import locutus.protocols.ring.contracts.ValueUpdate
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.ECSignature
import locutus.tools.crypto.ec.verify
import java.security.interfaces.ECPublicKey

class SignedContractV1(val key : ECPublicKey) : Contract() {
    override fun valid(store: GlobalStore, value: Value): Result<Unit> {
        return if (value is SignedValueV1) {
            if (value.verify(key)) Result.success(Unit) else Result.failure(Throwable("Signature failed to verify"))
        } else {
            Result.failure(Throwable("Value isn't a SignedValueV1"))
        }
    }

    override fun update(old: Value, update: ValueUpdate): Value? {
        return if (old is SignedValueV1 && update is SignedValueUpdateV1) {
            if (update.newSignedValue.payload.version > old.payload.version) {
                update.newSignedValue
            } else {
                null
            }
        } else {
            null
        }
    }
}

@Serializable
class SignedValueV1(val signature : ECSignature, val payload: VersionedPayload) : Value() {
    private val serializedVersionedPayload : ByteArray by lazy {
        ProtoBuf.encodeToByteArray(VersionedPayload.serializer(), payload)
    }

    fun verify(key : ECPublicKey) : Boolean
        = key.verify(signature, serializedVersionedPayload)
}

@Serializable
class SignedValueUpdateV1(val newSignedValue : SignedValueV1) : ValueUpdate()

@Serializable
class VersionedPayload(val version : UInt, val payload : ByteArray)