package locutus.net

interface MessageTransport<Target : Any, Payload : Any> {
    fun send(to : Target, message : Payload)

    fun setReceiver(receiver : (sender : Target, message : Payload) -> Unit)
}