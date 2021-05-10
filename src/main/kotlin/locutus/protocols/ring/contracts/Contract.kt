package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import locutus.apps.flog.protocol.v1.FlogContractV1
import locutus.apps.flog.protocol.v1.FlogUpdateV1
import locutus.protocols.ring.store.GlobalStore
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@Serializable

abstract class Contract<V : Value, VU : ValueUpdate>(private val valueCls : KClass<V>) {
    open val updatable: Boolean get() = false

    fun valid(store: GlobalStore, p: Value): Boolean {
        return ivalid(store, p as V)
    }

    abstract fun ivalid(store : GlobalStore, p : V) : Boolean

    fun update(old : Value, update : ValueUpdate) : Value?
     = if (!valueCls.isInstance(old)) null else iupdate(old as V, update as VU)

    abstract fun iupdate(old: V, update: VU): V?

    val sig by lazy { ContractAddress.fromContract(this) }
}

val contractModule = SerializersModule {
    polymorphic(Contract::class) {
        subclass(FlogContractV1::class)
    }
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




