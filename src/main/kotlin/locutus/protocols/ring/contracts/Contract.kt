package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.apps.flog.protocol.v1.FlogContractV1
import locutus.apps.flog.protocol.v1.FlogUpdateV1
import locutus.protocols.ring.store.GlobalStore
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@Serializable

abstract class Contract {
    open val updatable: Boolean get() = false

    abstract fun valid(store: GlobalStore, p: Value): Boolean

    abstract fun update(old : Value, update : ValueUpdate) : Value?

    val sig by lazy { ContractAddress.fromContract(this) }
}

val contractModule = SerializersModule {
    polymorphic(Contract::class) {
        subclass(FlogContractV1::class)
    }
}

val contractProtoBuf = ProtoBuf {
    serializersModule = contractModule
}

@Serializable
abstract class ValueUpdate

val postUpdateModule = SerializersModule {
    polymorphic(ValueUpdate::class) {
        subclass(FlogUpdateV1::class)
    }
}

@Serializable
abstract class Value




