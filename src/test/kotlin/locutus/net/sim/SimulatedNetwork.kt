package locutus.net.sim

import locutus.net.messages.Peer
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.net.InetSocketAddress

class SimulatedNetwork {
    private val portGenerator = java.util.concurrent.atomic.AtomicInteger(1024)

    val transports = ConcurrentHashMap<Int, SimulatedTransport>()

    fun createTransport(isOpen : Boolean): SimulatedTransport {
        val transport = SimulatedTransport(Peer(InetSocketAddress("localhost", portGenerator.getAndIncrement())), this, isOpen)
        transports[transport.peer.port] = transport
        return transport
    }

}