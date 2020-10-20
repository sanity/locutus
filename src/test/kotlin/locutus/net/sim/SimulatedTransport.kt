package locutus.net.sim

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import locutus.net.Transport
import locutus.net.messages.Peer

class SimulatedTransport internal constructor(val peer: Peer, val network: SimulatedNetwork, override val isOpen: Boolean) : Transport {
    override fun send(to: Peer, message: ByteArray) {
        runBlocking {
            network.transports.getValue(to.port).recepient.send(peer to message)
        }
    }

    override val recepient: Channel<Pair<Peer, ByteArray>> = Channel()
}

