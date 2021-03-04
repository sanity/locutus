package locutus.net

import com.google.common.cache.CacheBuilder
import locutus.net.messages.Message.Meta.LargeMessage
import locutus.net.messages.Peer
import java.time.Duration

class LargeMessageManager(val connectionManager : ConnectionManager) {

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

    class InboundMessageHandler(initialMessage : LargeMessage) {

        private val receivedMessages : Array<LargeMessage?>

        init {
            receivedMessages = Array(initialMessage.totalParts) { null }
            receivedMessages[initialMessage.partNo] = initialMessage
        }

        fun handle(message : LargeMessage) {

        }
    }

}

