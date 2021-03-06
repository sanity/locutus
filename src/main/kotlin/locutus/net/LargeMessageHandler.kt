package locutus.net

import com.google.common.cache.CacheBuilder
import locutus.Constants
import locutus.net.messages.Message
import locutus.net.messages.Message.Meta.LargeMessage
import locutus.net.messages.Message.Meta.LargeMessageResend
import locutus.net.messages.PartNo
import locutus.net.messages.Peer
import java.time.Duration
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.HashSet

private val MAX_PART_SIZE = Constants.MAX_UDP_PACKET_SIZE-100

class LargeMessageHandler(val connectionManager : ConnectionManager) {

    private val timer = Timer()

    private val inboundMessageHandlers = CacheBuilder
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .build<Int, InboundMessageHandler>()

    init {
        connectionManager.listen<LargeMessage> { sender, largeMessage ->

        }
    }

    fun send(to : Peer, serializedMessage : ByteArray) {

    }

    private fun handle(from : Peer, message : LargeMessage) {
        val handler = inboundMessageHandlers.get(message.uid) { InboundMessageHandler(from, message.totalParts) }

        val missingMessageParts : Set<PartNo> = handler
            .receivedMessages
            .asSequence()
            .withIndex()
            .takeWhile { it.index < message.partNo }
            .filter { it.value == null }
            .map { it.index }
            .toCollection(HashSet())
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

    class InboundMessageHandler(val sender : Peer, totalParts : Int) {

        @Volatile
        internal var nextMessageTimeout : TimerTask? = null

        internal val receivedMessages : CopyOnWriteArrayList<LargeMessage?> = CopyOnWriteArrayList(Array<LargeMessage?>(totalParts) { null })

        operator fun plusAssign(message : LargeMessage) {
            receivedMessages[message.partNo] = message
        }

        fun isComplete() : ByteArray? {
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

