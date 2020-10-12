package locutus.net.messages

class Extractor<MType : Message, KeyType : Any>(val label: String, val f : (MessageRouter.SenderMessage<MType>).() -> KeyType)