package locutus.net.messages

abstract class Extractor<MType : Message, KeyType : Any>(val label: String)
    : (MessageRouter.SenderMessage<MType>) -> KeyType {
    companion object {
        fun <MType : Message, KeyType : Any> create(
                label: String,
                block: (MessageRouter.SenderMessage<MType>).() -> KeyType
        ): Extractor<MType, KeyType> {
            return object : Extractor<MType, KeyType>(label) {
                override fun invoke(p1: MessageRouter.SenderMessage<MType>): KeyType {
                    return block(p1)
                }

            }
        }
    }
}