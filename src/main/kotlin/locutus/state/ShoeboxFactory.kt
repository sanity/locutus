package locutus.state

import kotlinx.serialization.KSerializer
import kweb.shoebox.Shoebox
import kweb.shoebox.stores.MapDBStore
import kweb.shoebox.stores.MemoryStore
import org.mapdb.DB

interface ShoeboxFactory {
    fun <V : Any> create(name : String, serializer : KSerializer<V>) : Shoebox<V>
}

class MapDBShoeboxFactory(val db: DB) : ShoeboxFactory {
    override fun <V : Any> create(name: String, serializer: KSerializer<V>)
        = Shoebox(MapDBStore(db, name, serializer))
}

class MemoryShoeboxFactory : ShoeboxFactory {
    override fun <V : Any> create(name: String, serializer: KSerializer<V>): Shoebox<V>
        = Shoebox(MemoryStore())

}