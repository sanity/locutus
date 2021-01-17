package locutus.net

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.messages.*
import locutus.net.messages.Message.Testing.BarMessage
import locutus.net.messages.Message.Testing.FooMessage
import locutus.tools.crypto.rsa.RSAKeyPair
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@ExperimentalSerializationApi
@Order(0)
class ConnectionManagerSpec : FunSpec({
    context("two non-open managers") {
        val cm1Port = 13255
        val cm2Port = 13256
        val cm1 = ConnectionManager(port = cm1Port, myKey = RSAKeyPair.create(), isOpen = false)
        val cm2 = ConnectionManager(port = cm2Port, myKey = RSAKeyPair.create(), isOpen = false)
        val peer2 = Peer(InetSocketAddress("localhost", cm2Port))
        cm1.addConnection(PeerKey(peer2, cm2.myKey.public), false)
        val peer1 = Peer(InetSocketAddress("localhost", cm1Port))
        cm2.addConnection(PeerKey(peer1, cm1.myKey.public), false)
        test("Send a FooMesage from peer1 to peer2 which is an initiate message") {
            val fooReceived = AtomicBoolean(false)
            cm2.listen(
                    Extractor<FooMessage, Peer>("fooExtractor") { sender },
                    peer1,
                    Duration.ofMillis(500)
            ) {
                receivedMessage.v shouldBe 12
                fooReceived.set(true)
            }
            cm1.send(peer2, FooMessage(12, true))
            eventually(1.seconds) {
                fooReceived.get() shouldBe true
            }
        }
        test("Peer2 responds with a BarMessage which is not an initiate message") {
            val barReceived = AtomicBoolean(false)
            cm1.listen(
                    Extractor<BarMessage, Peer>("barExtractor") { sender },
                    key = peer2,
                    timeout = Duration.ofMillis(500)
            ) {
                receivedMessage.n shouldBe "hello"
                barReceived.set(true)
            }
            cm2.send(peer1, BarMessage("hello"))
            eventually(1.seconds) {
                barReceived.get() shouldBe true
            }
        }
        test("Peer 1 sends another FooMessage") {
            val fooReceived = AtomicBoolean(false)
            cm2.listen(
                    Extractor<FooMessage, Peer>("fooExtractor") { sender },
                    peer1,
                    Duration.ofMillis(500)
            ) {
                receivedMessage.v shouldBe 56
                fooReceived.set(true)
            }
            cm1.send(peer2, FooMessage(56, true))
            eventually(1.seconds) {
                fooReceived.get() shouldBe true
            }
        }
    }

    context("Non-open to open") {
        val cm1Port = 13257
        val cm2Port = 13258
        val cm1 = ConnectionManager(port = cm1Port, myKey = RSAKeyPair.create(), isOpen = false)
        val cm2 = ConnectionManager(port = cm2Port, myKey = RSAKeyPair.create(), isOpen = true)
        val peer2 = Peer(InetSocketAddress("localhost", cm2Port))
        cm1.addConnection(PeerKey(peer2, cm2.myKey.public), unsolicited = true)
        val peer1 = Peer(InetSocketAddress("localhost", cm1Port))
        // cm2.addConnection(PeerKey(peer1, cm1.myKey.public), false)
        test("Send a FooMesage from peer1 to peer2 which is an initiate message") {
            val fooReceived = AtomicBoolean(false)
            cm2.listen(Extractor<FooMessage, Peer>("fooExtractor") { sender }, peer1, Duration.ofMillis(500)) {
                receivedMessage.v shouldBe 12
                fooReceived.set(true)
            }
            cm1.send(peer2, FooMessage(12, true))
            eventually(1.seconds) {
                fooReceived.get() shouldBe true
            }
        }
        test("Peer2 responds with a BarMessage which is not an initiate message") {
            val barReceived = AtomicBoolean(false)
            cm1.listen(Extractor<BarMessage, Peer>("barExtractor") { sender  }, key = peer2, timeout = Duration.ofMillis(500)) {
                receivedMessage.n shouldBe "hello"
                barReceived.set(true)
            }
            cm2.send(peer1, BarMessage("hello"))
            eventually(1.seconds) {
                barReceived.get() shouldBe true
            }
        }
        test("Peer 1 sends another FooMessage") {
            val fooReceived = AtomicBoolean(false)
            cm2.listen(
                    Extractor<FooMessage, Peer>("fooExtractor") { sender },
                    peer1,
                    Duration.ofMillis(500)
            ) {
                receivedMessage.v shouldBe 56
                fooReceived.set(true)
            }
            cm1.send(peer2, FooMessage(56, true))
            eventually(1.seconds) {
                fooReceived.get() shouldBe true
            }
        }
    }
})