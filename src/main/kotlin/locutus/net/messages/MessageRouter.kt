package locutus.net.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

@PublishedApi internal val logger = KotlinLogging.logger {}

class MessageRouter {

    @PublishedApi
    internal val scope = MainScope()

    data class SenderMessage<MType : Message>(val sender: Peer, val message: MType)

    inline fun <reified MType : Message, KeyType : Any> listen(
            for_: Extractor<MType, KeyType>,
            key: KeyType,
            timeout: Duration?,
            noinline block: (MessageReceiver<MType>).() -> Unit
    ) {
        logger.info("Add listener for ${MType::class.simpleName}")
        listeners
                .computeIfAbsent(MType::class) { ConcurrentHashMap() }
                .computeIfAbsent(for_.label) { ConcurrentHashMap() }[key] = block as MessageReceiver<*>.() -> Unit
        extractors.putIfAbsent(for_.label, for_ as Extractor<Message, Any>)
        if (timeout != NEVER) {
            scope.launch(Dispatchers.IO) {
                delay(timeout)
                val extractorMap = listeners[MType::class]
                if (extractorMap != null) {
                    val receiverMap = extractorMap[for_.label]
                    if (receiverMap != null) {
                        receiverMap.remove(key)
                        if (receiverMap.isEmpty()) {
                            extractorMap.remove(for_.label)
                        }
                    }
                }
            }
        }
    }

    fun route(sender: Peer, message: Message) {
        val classListeners = listeners[message::class]
        if (classListeners == null) {
            logger.info("No listener found for message type ${message::class.simpleName}")
            return
        }
        val messageHandled = AtomicBoolean(false)
        for ((extractorLabel, keys) in classListeners.entries) {
            val extractor = extractors[extractorLabel]
            requireNotNull(extractor) { "No extractor found for label $extractorLabel" }
            val receiver = keys[extractor.extractor.invoke(SenderMessage(sender, message))]
            receiver?.let { receiver ->
                messageHandled.set(true)
                receiver.invoke(object : MessageReceiver<Message> {
                    override val sender = sender
                    override val received: Message = message
                })
            }
        }
        if (!messageHandled.get()) {
            logger.warn { "No listener found for message: $message" }
        }
    }

    @PublishedApi
    internal val listeners = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<ExtractorLabel,
                    ConcurrentHashMap<KeyType, (MessageReceiver<*>).() -> Unit>>>()

    @PublishedApi
    internal val extractors = ConcurrentHashMap<String, Extractor<Message, Any>>()
}

interface MessageReceiver<ReceivedMessageType : Message> {
    val sender: Peer

    val received: ReceivedMessageType
}

typealias ExtractorLabel = String
typealias KeyType = Any

val NEVER = null