package locutus.net.messages

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap

private val usedLabels = ConcurrentHashMap<String, Unit>()

class Extractor<MType : Message, KeyType : Any>(
        val label: String,
        val extractor: (MessageRouter.SenderMessage<MType>).() -> KeyType
) {
    init {
        require(label !in usedLabels) { "Attempted to create more than one Extractor using label $label" }
        usedLabels[label] = Unit
    }
}