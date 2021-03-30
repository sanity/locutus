package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import locutus.net.messages.Bytes
import locutus.protocols.ring.store.GlobalStore

@Serializable
sealed class Contract {
    abstract fun valid(store: GlobalStore, p: Post): Boolean

    abstract fun supersedes(old: Post, new: Post): Boolean

    val sig by lazy { ContractAddress.fromContract(this) }
}
