package locutus.net

import locutus.net.messages.Message

class LargeMessageHandler(val connectionManager : ConnectionManager) {

    init {
        connectionManager.listen<Message.Meta.LargeMessage> { sender, largeMessage ->

        }
    }

}