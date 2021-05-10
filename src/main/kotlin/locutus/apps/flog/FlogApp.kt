package locutus.apps.flog

import kotlinx.serialization.KSerializer
import locutus.net.ConnectionManager
import locutus.protocols.ring.contracts.Contract

class FlogApp<PostType : Any>(val connectionManager: ConnectionManager, serializer : KSerializer<PostType>) {

}
