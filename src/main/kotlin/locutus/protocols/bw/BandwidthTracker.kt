package locutus.protocols.bw

import locutus.net.ConnectionManager
import locutus.net.MessageListener
import locutus.net.messages.Peer
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.io.Closeable

class BandwidthTracker(val cm : ConnectionManager) : Closeable {

    val global = Rates()

    private val peers = ConcurrentHashMap<Peer, Rates>()

    private val messageListenerHandle = cm.addListener { sent, _, raw, other ->
        val bytes = raw.size
        if (sent == MessageListener.Type.SENT) {
            global.upload.registerTraffic(bytes)
            peers.computeIfAbsent(other) { Rates() }.upload.registerTraffic(bytes)
        } else {
            global.download.registerTraffic(bytes)
            peers.computeIfAbsent(other) { Rates() }.download.registerTraffic(bytes)
        }
    }

    operator fun get(peer : Peer) = peers[peer]

    override fun close() {
        cm.removeListener(messageListenerHandle)
    }


}

class Rates(val upload : Rate = Rate(), val download : Rate = Rate()) {
    val max: BytesPerSecond get() = maxOf(upload.rate(), download.rate())
}

typealias BytesPerSecond = Double

