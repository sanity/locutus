package locutus.net.messages

class Extractor<MType : Message, KeyType : Any>(
        val label: String,
        val extractor: (MessageRouter.SenderMessage<MType>).() -> KeyType
)