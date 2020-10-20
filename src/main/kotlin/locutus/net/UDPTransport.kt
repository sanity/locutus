package locutus.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import locutus.Constants
import locutus.net.messages.Peer
import mu.KotlinLogging
import mu.withLoggingContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.concurrent.thread

class UDPTransport(val port : Int, override val isOpen: Boolean) :Transport {
    private val logger = KotlinLogging.logger {}

    private val scope = MainScope()

    private val channel: DatagramChannel = DatagramChannel.open()

    init {
        withLoggingContext("port" to port.toString()) {
            logger.info { "Listening on UDP port $port" }
            startListenThread()
        }
    }


    override fun send(to: Peer, message: ByteArray) {
        channel.send(ByteBuffer.wrap(message), to.asSocketAddress)
    }

    override val recepient : Channel<Pair<Peer, ByteArray>> = Channel()

    private fun startListenThread() {
        channel.socket().bind(InetSocketAddress(port))
        val buf = ByteBuffer.allocateDirect(Constants.MAX_UDP_PACKET_SIZE + 200)
        thread {
            while (true) {
                val sender = Peer(channel.receive(buf) as InetSocketAddress)
                logger.debug { "Packet received from $sender of length ${buf.remaining()}" }
                buf.flip()
                val byteArray = ByteArray(buf.remaining())
                buf.get(byteArray)
                buf.clear()
                scope.launch(Dispatchers.IO) {
                    recepient.send(sender to byteArray)
                }
            }
        }
    }

}