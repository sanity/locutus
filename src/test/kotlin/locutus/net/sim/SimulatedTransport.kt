package locutus.net.sim

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import locutus.net.Transport
import locutus.net.messages.Peer

private val scope = MainScope()

class SimulatedTransport internal constructor(val peer: Peer, val network: SimulatedNetwork, override val isOpen: Boolean) : Transport {
    override fun send(to: Peer, message: ByteArray) {
        scope.launch(Dispatchers.IO) {
            network.transports.getValue(to.port).recepient.send(peer to message)
        }
    }

    override val recepient: Channel<Pair<Peer, ByteArray>> = Channel(capacity = 100)
}

