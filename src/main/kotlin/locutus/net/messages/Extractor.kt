package locutus.net.messages

import locutus.net.retriever.Request
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import kotlin.reflect.KProperty1

private val usedLabels = ConcurrentHashMap<String, Unit>()

open class Extractor<MType : Message, KeyType : Any>(
        val label: String,
        val extractor: (sender : Peer, message : MType) -> KeyType
) {
    init {
        require(label !in usedLabels) { "Attempted to create more than one Extractor using label $label" }
        usedLabels[label] = Unit
    }

    companion object {
        fun <M : Message, K : Any> property(p : KProperty1<M, K>) : Extractor<M, K> {
            return Extractor(p.name) { _, message -> p(message) }
        }
    }
}

class ReplyExtractor<MType : Message>(label: String) : Extractor<MType, PeerId>(label, { sender, message ->
    if (message is Reply) {
        PeerId(sender, message.replyTo)
    } else {
        error("ReplyExtractor can only be used with messages that implement Reply interface")
    }
}) {

}

data class PeerId(val peer : Peer, val id : MessageId)