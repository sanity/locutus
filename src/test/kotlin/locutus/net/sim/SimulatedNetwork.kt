package locutus.net.sim

import locutus.net.messages.Peer
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.net.InetSocketAddress

class SimulatedNetwork {
    private val port = java.util.concurrent.atomic.AtomicInteger(1024)

    internal val transports = ConcurrentHashMap<Int, SimulatedTransport>()

    fun createTransport(isOpen : Boolean): SimulatedTransport {
        return SimulatedTransport(Peer(InetSocketAddress("localhost", port.getAndIncrement())), this, isOpen)
    }

}