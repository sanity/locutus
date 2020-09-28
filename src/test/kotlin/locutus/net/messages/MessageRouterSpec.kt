package locutus.net.messages

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.launch
import locutus.net.messages.Message.Testing.BarMessage
import locutus.net.messages.Message.Testing.FooMessage
import locutus.net.messages.MessageRouter.*
import java.net.*
import java.util.concurrent.*
import kotlin.time.*

@ExperimentalTime
class MessageRouterSpec : FunSpec({
    context("Given a MessageRouter and an extractor for FooMessage") {
        val messageRouter = MessageRouter()
        val fooExtractor = object : Extractor<FooMessage, Int>("fooExtractor") {
            override fun invoke(p1: SenderMessage<FooMessage>) = p1.message.v
        }

        val fooReceived = ConcurrentLinkedQueue<SenderMessage<FooMessage>>()

        context("Create listener for FooMessage(1) that will cancel after initial message is received") {
            val channel = messageRouter.listen(fooExtractor, 1)

            test("Listener should have been added to MessageRouter") {
                messageRouter.listeners[FooMessage::class]?.get("fooExtractor")?.size shouldBe 1
            }

            launch {
                channel.consume {
                    fooReceived += this.receive()
                }
            }

            val sender = Peer(InetSocketAddress(InetAddress.getLocalHost(), 1234))

            test("Wrong message type should be ignored") {
                messageRouter.route(sender, BarMessage("ignored"))
                eventually(100.milliseconds) {
                    fooReceived.isEmpty() shouldBe true
                }
            }

            test("FooMessage(2) should be ignored") {
                messageRouter.route(sender, FooMessage(2))
                eventually(100.milliseconds) {
                    fooReceived.isEmpty() shouldBe true
                }
            }

            test("FooMessage(1) should be added to fooReceived") {
                messageRouter.route(sender, FooMessage(1))
                eventually(100.milliseconds) {
                    fooReceived shouldContainExactly listOf(SenderMessage(sender, FooMessage(1)))
                }
            }

            test("Channel should be closed") {
                channel.isClosedForReceive shouldBe true
            }

            test("Subsequent FooMessage(1) should be ignored because listener was cancelled") {
                messageRouter.route(sender, FooMessage(1))
                eventually(100.milliseconds) {
                    fooReceived shouldContainExactly listOf(SenderMessage(sender, FooMessage(1)))
                }
            }

            test("Listener should have been removed from MessageRouter") {
                messageRouter.listeners[FooMessage::class]?.get("fooExtractor")?.isNullOrEmpty() shouldBe true
            }
        }
    }
})
