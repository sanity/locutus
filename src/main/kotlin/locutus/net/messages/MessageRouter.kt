package locutus.net.messages

import kotlinx.coroutines.channels.*
import locutus.net.messages.Message.Ring.JoinRequest
import locutus.net.messages.MessageRouter.*
import mu.KotlinLogging
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.net.InetSocketAddress
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class MessageRouter {

    abstract class Extractor<MType : Message, KeyType : Any>(val label : String) : (SenderMessage<MType>) -> KeyType

    data class SenderMessage<MType : Message>(val sender : Peer, val message : MType)

    /**
     * Cancelling the ReceiveChannel will result in the removal of the key listener
     */
    inline fun <reified MType : Message, KeyType : Any> add(
        extractor: Extractor<MType, KeyType>,
        key: KeyType
    ) : ReceiveChannel<SenderMessage<MType>> {
        val channel : Channel<SenderMessage<MType>> = listeners
            .computeIfAbsent(MType::class) { ConcurrentHashMap() }
            .computeIfAbsent(extractor.label) { ConcurrentHashMap() }
            .compute(key) { _, existingChannel ->
                if (existingChannel != null) {
                    error("Listener already exists for ${extractor.label}->$key")
                } else {
                    Channel()
                }
            } as Channel<SenderMessage<MType>>
        extractors.putIfAbsent(extractor.label, extractor as Extractor<Message, Any>)
        channel.invokeOnClose {
            listeners[MType::class]?.get(extractor.label)?.remove(key)
        }
        return channel
    }

    suspend fun route(sender : Peer, message : Message) {
        val classListeners = listeners[message::class]
        if (classListeners == null) {
            logger.warn("No listener found for message $message")
            return
        }
        for ((extractorLabel, keys) in classListeners.entries) {
            val extractor = extractors[extractorLabel]
            requireNotNull(extractor) { "No extractor found for label $extractorLabel" }
            val channel = keys[extractor.invoke(SenderMessage(sender, message))]
            channel?.send(SenderMessage(sender, message))
        }
    }

    @PublishedApi
    internal val listeners = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<ExtractorLabel,
                    ConcurrentHashMap<KeyType, SendChannel<SenderMessage<Message>>>>>()

    @PublishedApi
    internal val extractors = ConcurrentHashMap<String, Extractor<Message, Any>>()
}

suspend fun tst() {
    val mr = MessageRouter()
    val joinRequestExtractor = object : Extractor<JoinRequest, Peer>("joinRequestExtractor") {
        override fun invoke(sm : MessageRouter.SenderMessage<JoinRequest>) = sm.sender
    }
    mr.add(joinRequestExtractor, Peer(InetSocketAddress.createUnresolved("localhost", 1233))).consume {
        for ((sender, joinRequest) in this) {

        }
    }
}

private typealias ExtractorLabel = String
private typealias KeyType = Any