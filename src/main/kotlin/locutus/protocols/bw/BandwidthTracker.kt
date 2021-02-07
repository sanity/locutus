package locutus.protocols.bw

import kweb.state.KVar
import locutus.net.ConnectionManager
import locutus.net.MessageListener
import locutus.net.messages.Peer
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.io.Closeable
import java.time.Duration
import java.time.Instant

private val sensitivity = 0.1

class BandwidthTracker(val cm : ConnectionManager) : Closeable {

    private val global = Rates()

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

    fun rates(peer : Peer) = peers[peer]

    override fun close() {
        cm.removeListener(messageListenerHandle)
    }


}

class Rates(val upload : Rate = Rate(), val download : Rate = Rate()) {
    /**
     * Ratio of downloads to uploads, eg 2.0 indicates twice as much downloaded as uploaded
     */
    fun ratio() : Double = download.rate.value / upload.rate.value
}

typealias BytesPerSecond = Double

class Rate(val lastMessageTime : KVar<Instant> = KVar(Instant.now()), val rate : KVar<BytesPerSecond> = KVar(0.0)) {
    fun registerTraffic(bytes : Int) {
        val duration = Duration.between(lastMessageTime.value, Instant.now())
        val r = bytes.toDouble() / duration.seconds.toDouble()
        this.rate.value = (this.rate.value * (1.0 - sensitivity)) + (r * sensitivity)
    }
}