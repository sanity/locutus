package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import locutus.protocols.microblog.v1.MicroblogContractV1
import locutus.protocols.microblog.v1.MicroblogPostV1
import locutus.protocols.microblog.v1.MicroblogUpdateV1
import locutus.protocols.ring.store.GlobalStore

@Serializable
abstract class Contract {
    open val updatable: Boolean get() = false

    abstract fun valid(store: GlobalStore, p: Post): Boolean

    abstract fun update(old: Post, update: PostUpdate): Post?

    val sig by lazy { ContractAddress.fromContract(this) }
}

val contractModule = SerializersModule {
    polymorphic(Contract::class) {
        subclass(MicroblogContractV1::class)
    }
}

@Serializable
abstract class PostUpdate

val postUpdateModule = SerializersModule {
    polymorphic(PostUpdate::class) {
        subclass(MicroblogUpdateV1::class)
    }
}

@Serializable
abstract class Post




