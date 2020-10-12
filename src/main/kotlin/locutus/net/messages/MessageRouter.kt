package locutus.net.messages

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.time.Duration
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class MessageRouter {

    @PublishedApi internal val scope = MainScope()

    data class SenderMessage<MType : Message>(val sender : Peer, val message : MType)

    inline fun <reified MType : Message, KeyType : Any> listen(
            extractor: Extractor<MType, KeyType>,
            key: KeyType,
            timeout : Duration?,
            noinline block : (MessageReceiver<MType>).() -> Unit
    ) {
        listeners
            .computeIfAbsent(MType::class) { ConcurrentHashMap() }
            .computeIfAbsent(extractor.label) { ConcurrentHashMap() }[key] = block as MessageReceiver<*>.() -> Unit
        extractors.putIfAbsent(extractor.label, extractor as Extractor<Message, Any>)
        if (timeout != NEVER) {
            scope.launch(Dispatchers.IO) {
                delay(timeout)
                val extractorMap = listeners[MType::class]
                if (extractorMap != null) {
                    val receiverMap = extractorMap[extractor.label]
                    if (receiverMap != null) {
                        receiverMap.remove(key)
                        if (receiverMap.isEmpty()) {
                            extractorMap.remove(extractor.label)
                        }
                    }
                }
            }
        }
    }

    fun route(sender : Peer, message : Message) {
        val classListeners = listeners[message::class]
        if (classListeners == null) {
            logger.warn("No listener found for message $message")
            return
        }
        for ((extractorLabel, keys) in classListeners.entries) {
            val extractor = extractors[extractorLabel]
            requireNotNull(extractor) { "No extractor found for label $extractorLabel" }
            val receiver = keys[extractor.invoke(SenderMessage(sender, message))]
            receiver?.invoke(object : MessageReceiver<Message> {
                override val sender = sender
                override val message: Message = message
            })
        }
    }

    @PublishedApi
    internal val listeners = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<ExtractorLabel,
                    ConcurrentHashMap<KeyType, (MessageReceiver<*>).() -> Unit>>>()

    @PublishedApi
    internal val extractors = ConcurrentHashMap<String, Extractor<Message, Any>>()
}

interface MessageReceiver<MType : Message> {
    val sender : Peer

    val message : MType
}

typealias ExtractorLabel = String
typealias KeyType = Any
val NEVER = null