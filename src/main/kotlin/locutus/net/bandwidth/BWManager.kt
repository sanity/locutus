package locutus.net.bandwidth

import locutus.net.ConnectionManager
import java.io.Closeable

class BWManager(val cm : ConnectionManager) : Closeable {

    private val messageListenerUid = cm.addListener { sent, message, raw, other ->

    }

    override fun close() {
        cm.removeListener(messageListenerUid)
    }


}