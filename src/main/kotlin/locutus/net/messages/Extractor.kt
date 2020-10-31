package locutus.net.messages

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap

private val usedLabels = ConcurrentHashMap<String, Unit>()

open class Extractor<MType : Message, KeyType : Any>(
        val label: String,
        val extractor: (MessageRouter.SenderMessage<MType>).() -> KeyType
) {
    init {
        require(label !in usedLabels) { "Attempted to create more than one Extractor using label $label" }
        usedLabels[label] = Unit
    }
}

class ReplyExtractor<MType : Message>(label: String) : Extractor<MType, PeerId>(label, {
    if (message is Reply) {
        PeerId(sender, message.replyTo)
    } else {
        error("ReplyExtractor can only be used with messages that implement Reply interface")
    }
}) {

}

data class PeerId(val peer : Peer, val id : MessageId)