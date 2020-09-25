package locutus.net

import locutus.net.messages.Peer

class UDPTransport(val port : Int) : MessageTransport<Peer, ByteArray> {
    override fun send(to: Peer, message: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun setReceiver(receiver: (sender: Peer, message: ByteArray) -> Unit) {
        TODO("Not yet implemented")
    }

}