package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import locutus.protocols.flog.v1.FlogContractV1
import locutus.protocols.flog.v1.FlogUpdateV1
import locutus.protocols.ring.store.GlobalStore
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@Serializable
abstract class Contract<P : Post, PU : PostUpdate>(private val postCls : KClass<P>) {
    open val updatable: Boolean get() = false

    fun valid(store: GlobalStore, p: Post): Boolean {
        return ivalid(store, p as P)
    }

    abstract fun ivalid(store : GlobalStore, p : P) : Boolean

    fun update(old : Post, update : PostUpdate) : Post?
     = if (!postCls.isInstance(old)) null else iupdate(old as P, update as PU)

    abstract fun iupdate(old: P, update: PU): P?

    val sig by lazy { ContractAddress.fromContract(this) }
}

val contractModule = SerializersModule {
    polymorphic(Contract::class) {
        subclass(FlogContractV1::class)
    }
}

@Serializable
abstract class PostUpdate

val postUpdateModule = SerializersModule {
    polymorphic(PostUpdate::class) {
        subclass(FlogUpdateV1::class)
    }
}

@Serializable
abstract class Post




