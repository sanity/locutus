package locutus.protocols.bw

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import locutus.net.ConnectionManager
import locutus.net.messages.Extractor
import locutus.net.messages.Message
import locutus.net.messages.NEVER
import locutus.net.messages.Peer
import java.time.Duration

class BWProtocol(val cm : ConnectionManager) {

    private val bandwidthTracker = BandwidthTracker(cm)

    private val lastBWLimitMsgs = CacheBuilder
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<Peer, Message.BW.BWLimit>()

    init {
        cm.listen(
            for_ = Extractor<Message.BW.BWLimit, Unit>("bwLimit") {},
            key = Unit,
            timeout = NEVER
            ) { requestor, bwLimit ->
                lastBWLimitMsgs.put(requestor, bwLimit)
        }
    }

    fun getBWLimit(peer : Peer) = lastBWLimitMsgs.getIfPresent(peer)

}