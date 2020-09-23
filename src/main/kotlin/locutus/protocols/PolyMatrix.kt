package locutus.protocols

import locutus.contracts.*
import locutus.net.ConnectionManager
import locutus.net.messages.Message.*
import locutus.net.messages.PeerWithKey


class PolyMatrix(private val cm : ConnectionManager, private val openPeers : Set<PeerWithKey>) {
    init {

    }

    suspend fun <S : Any, C : Contract<S, C>> search(key : Key<S, C>) {

    }
}