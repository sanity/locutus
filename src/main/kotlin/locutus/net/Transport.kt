package locutus.net

import kotlinx.coroutines.channels.ReceiveChannel
import locutus.net.messages.Peer

interface Transport {
    fun send(to : Peer, message : ByteArray)

    val isOpen : Boolean

    val recepient : ReceiveChannel<Pair<Peer, ByteArray>>
}