package locutus.protocols.bw

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import locutus.net.ConnectionManager
import locutus.net.messages.Message
import locutus.net.messages.Peer
import locutus.protocols.ring.RingProtocol
import java.time.Duration

/*class BandwidthManagementProtocol(
    val cm: ConnectionManager,
    val bandwidthTracker: BandwidthTracker,
    val ringProtocol: RingProtocol,
    val updateEvery: Duration = Duration.ofMinutes(1)
) {

    private val scope = MainScope()

    private val lastRateLimitMessages = CacheBuilder
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<Peer, Message.Meta.RateLimit>()

    init {
        cm.assertUnique(this::class)

        cm.listen<Message.Meta.RateLimit> { requestor, rateLimit ->
                lastRateLimitMessages.put(requestor, rateLimit)
        }

        scope.launch(Dispatchers.IO) {
            while (true) {
                val peers = ringProtocol.connectionsByLocation.values
                val perPeerDuration : Duration = updateEvery.dividedBy(peers.size.toLong())
                for (peerKeyLocation in peers) {
                    // Use PAV to determine relationship between per-peer limit and actual BW usage

                    delay(perPeerDuration)
                }
            }
        }
    }

    fun getBWLimit(peer : Peer) = lastRateLimitMessages.getIfPresent(peer)

}

private data class BandwidthLimitSample(val bwLimit : Duration, )*/

