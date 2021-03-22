package locutus.net

import com.google.common.cache.CacheBuilder
import kweb.util.random
import locutus.Constants
import locutus.net.messages.Message.Meta.LargeMessage
import locutus.net.messages.Message.Meta.LargeMessageResend
import locutus.net.messages.PartNo
import locutus.net.messages.Peer
import locutus.tools.PartTracker
import locutus.tools.crypto.split
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayDeque
import kotlin.collections.HashSet

typealias BytesPerSecond = Double

private const val MAX_PART_SIZE = Constants.MAX_UDP_PACKET_SIZE - 100

// TODO: Streaming of large messages
//  This will probably require a separate Handler

class LargeMessageHandler(val connectionManager: ConnectionManager) {

    private val timer = Timer()

    private val inboundMessageHandlers = CacheBuilder
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .build<Int, InboundMessageHandler>()

    init {
        connectionManager.assertUnique(this::class)

        connectionManager.listen<LargeMessage> { sender, largeMessage ->
            val handler = inboundMessageHandlers.asMap().computeIfAbsent(largeMessage.uid) {
                InboundMessageHandler(sender, largeMessage.totalParts)
            }.plusAssign(largeMessage)
        }
    }

    class PeerMessage(val to : Peer, val serializedMessage: ByteArray)

    private val messageQueue = ConcurrentLinkedDeque<PeerMessage>()

    fun send(to: Peer, serializedMessage: ByteArray, sendRate: BytesPerSecond) {
        val splitMessage = serializedMessage.split(MAX_PART_SIZE)
        val lmUid = random.nextInt()
        splitMessage.withIndex().forEach { (ix, payload) ->
            val expectNextMessageBy : Duration = Duration.ofMillis(((1000 * payload.size.toDouble() / sendRate).toLong()) )
            val lm = LargeMessage(lmUid, serializedMessage.size, 0, splitMessage.size, payload, expectNextMessageBy, ix == 0)
        }
    }

    private fun handle(from: Peer, message: LargeMessage) {
        val handler = inboundMessageHandlers.get(message.uid) { InboundMessageHandler(from, message.totalParts) }

        val missingMessageParts = handler.partTracker.missingRanges(message.partNo)
        if (missingMessageParts.isNotEmpty()) {
            connectionManager.send(handler.sender, LargeMessageResend(message.uid, missingMessageParts, message.partNo))
        }

        handler.nextMessageTimeout?.cancel()
        if (message.expectNextMessageBy != null) {
            handler.nextMessageTimeout = object : TimerTask() {
                override fun run() {

                }
            }

            timer.schedule(handler.nextMessageTimeout, message.expectNextMessageBy.toMillis())
        }
    }

    class InboundMessageHandler(val sender: Peer, totalParts: Int) {

        @Volatile
        internal var nextMessageTimeout: TimerTask? = null

        internal val receivedMessages: CopyOnWriteArrayList<LargeMessage?> = CopyOnWriteArrayList(Array<LargeMessage?>(totalParts) { null })

        internal val partTracker = PartTracker()

        operator fun plusAssign(message: LargeMessage) {
            receivedMessages[message.partNo] = message
            partTracker += message.partNo
        }

        fun isComplete(): ByteArray? {
            // TODO: Scanning receivedMessages every time is inefficient
            if (receivedMessages.none { it == null }) {
                val firstMessage = receivedMessages.first() ?: return ByteArray(0)
                val largeMessage = ByteArray(receivedMessages.first()!!.totalSize)
                var position = 0
                for (lm in receivedMessages) {
                    requireNotNull(lm)
                    lm.payload.copyInto(largeMessage, position)
                    position += lm.payload.size
                }
                return largeMessage
            } else {
                return null
            }
        }

    }

}

