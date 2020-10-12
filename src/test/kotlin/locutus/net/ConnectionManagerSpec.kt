package locutus.net

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.messages.Message
import locutus.net.messages.MessageRouter
import locutus.net.messages.Peer
import locutus.net.messages.PeerKey
import locutus.tools.crypto.rsa.RSAKeyPair
import java.net.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@ExperimentalSerializationApi
class ConnectionManagerSpec : FunSpec({
    context("two non-open managers") {
        val cm1 = ConnectionManager(port = 13255, myKey = RSAKeyPair.create(), false)
        val cm2 = ConnectionManager(port = 13256, myKey = RSAKeyPair.create(), false)
        val peer2 = Peer(InetSocketAddress("localhost", cm2.port))
        cm1.addConnection(PeerKey(peer2, cm2.myKey.public), false)
        val peer1 = Peer(InetSocketAddress("localhost", cm1.port))
        cm2.addConnection(PeerKey(peer1, cm1.myKey.public), false)
        val fooReceived = AtomicBoolean(false)
        cm2.listen(object : MessageRouter.Extractor<Message.Testing.FooMessage, Peer>("fooExtractor") {
            override fun invoke(p1: MessageRouter.SenderMessage<Message.Testing.FooMessage>): Peer = p1.sender
        }, peer1, Duration.ofMillis(500)) {
            fooReceived.set(true)
        }
        cm1.send(peer2, Message.Testing.FooMessage(12, false))
        eventually(1.seconds) {
            fooReceived.get() shouldBe true
        }
    }
})