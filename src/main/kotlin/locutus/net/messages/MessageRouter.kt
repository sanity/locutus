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
import kotlin.reflect.KProperty1

@PublishedApi internal val logger = KotlinLogging.logger {}

class MessageRouter {

    @PublishedApi
    internal val scope = MainScope()

    data class SenderMessage<MType : Message>(val sender: Peer, val message: MType)

    inline fun <reified MType : Message, KeyType : Any> listen(
            for_: Extractor<MType, KeyType>,
            key: KeyType,
            timeout: Duration?,
            noinline block: (from : Peer, message : MType) -> Unit
    ) {
        listen(MType::class, for_, key, timeout, block)
    }

    fun <KeyType : Any, MType : Message> listen(
        msgKClass: KClass<MType>,
        property: KProperty1<MType, locutus.net.messages.KeyType>,
        key: KeyType,
        timeout: Duration?,
        block: (from: Peer, message: MType) -> Unit
    ) {
        logger.info("Add listener for ${msgKClass.simpleName}")
        listeners
            .computeIfAbsent(msgKClass) { ConcurrentHashMap() }
            .computeIfAbsent(for_.label) { ConcurrentHashMap() }[key] = block as ((from: Peer, message: Message) -> Unit)
        extractors.putIfAbsent(for_.label, for_ as Extractor<Message, Any>)
        if (timeout != NEVER) {
            scope.launch(Dispatchers.IO) {
                delay(timeout)
                val extractorMap = listeners[msgKClass]
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
            val receiver = keys[extractor.extractor.invoke(sender, message)]
            receiver?.let { receiver ->
                messageHandled.set(true)
                receiver.invoke(sender, message)
            }
        }
        if (!messageHandled.get()) {
            logger.warn { "No listener found for message: $message" }
        }
    }

    @PublishedApi
    internal val listeners = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<ExtractorLabel,
                    ConcurrentHashMap<KeyType, (from : Peer, message : Message) -> Unit>>>()

    @PublishedApi
    internal val extractors = ConcurrentHashMap<String, Extractor<Message, Any>>()
}

typealias ExtractorLabel = String
typealias KeyType = Any

val NEVER = null