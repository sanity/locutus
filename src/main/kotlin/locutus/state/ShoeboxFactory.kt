package locutus.state

import kotlinx.serialization.KSerializer
import kweb.shoebox.Shoebox
import kweb.shoebox.stores.MapDBStore
import kweb.shoebox.stores.MemoryStore
import locutus.tools.Bytes
import mu.KotlinLogging
import org.mapdb.DB

private val logger = KotlinLogging.logger {}

interface ShoeboxFactory {
    fun <V : Any> create(name : String, serializer : KSerializer<V>, maxSize: Bytes? = null) : Shoebox<V>
}

class MapDBShoeboxFactory(val db: DB) : ShoeboxFactory {
    override fun <V : Any> create(name: String, serializer: KSerializer<V>, maxSize : Bytes?)
        = Shoebox(MapDBStore(db, name, serializer, maxSize))
}

class MemoryShoeboxFactory : ShoeboxFactory {
    override fun <V : Any> create(name: String, serializer: KSerializer<V>, maxSize: Bytes?): Shoebox<V> {
        if (maxSize != null) {
            logger.warn { "maxSize will be ignored for MemoryStore" }
        }
        return Shoebox(MemoryStore())
    }

}