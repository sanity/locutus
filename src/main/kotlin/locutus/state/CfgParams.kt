package locutus.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kweb.shoebox.Shoebox
import kweb.shoebox.stores.MapDBStore
import org.mapdb.DB

class CfgParams(shoeboxFactory: ShoeboxFactory) {
    val store = shoeboxFactory.create("cfg_param", CfgParam.serializer())

    operator fun plusAssign(param : CfgParam) {
        store[param.name] = param
    }

    operator fun minusAssign(param : CfgParam) {
        store.remove(param.name)
    }

    fun <T : Any> get(name : String, serializer : KSerializer<T>) : T? {
        val v = store[name]?.value ?: return null
        return ProtoBuf.decodeFromByteArray(serializer, v)
    }

    fun <T : Any> computeIfAbsent(name : String, serializer: KSerializer<T>, gen : () -> T) : T {
        val r = get(name, serializer)
        return if (r == null) {
            val n = gen()
            store[name] = CfgParam(name, ProtoBuf.encodeToByteArray(serializer, n))
            n
        } else {
            r
        }
    }

    @Serializable class CfgParam(
        val name : String,
        val value : ByteArray
    )
}
