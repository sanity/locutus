package locutus.state

import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import org.mapdb.Serializer

class MapDB(val filename : String = "locutus.db") {

    val db = DBMaker
        .fileDB(filename)
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
}