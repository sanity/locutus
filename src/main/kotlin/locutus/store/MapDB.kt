package locutus.store

import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import org.mapdb.Serializer

object MapDB {

    val db = DBMaker
        .fileDB("locutus.db")
        .transactionEnable()
        .make()

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                db.commit()
                db.close()
            }
        })
    }

    fun newTable(name : String): HTreeMap<String, ByteArray> {
        return MapDB.db.hashMap(name).keySerializer(Serializer.STRING).valueSerializer(Serializer.BYTE_ARRAY).create()
    }
}