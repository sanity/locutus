package locutus.net.messages

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import locutus.net.messages.Message.Testing.BarMessage
import locutus.net.messages.Message.Testing.FooMessage
import locutus.net.messages.MessageRouter.*
import java.net.*
import java.util.concurrent.*
import kotlin.time.*

@ExperimentalTime
@Order(0)
class MessageRouterSpec : FunSpec({
    context("Given a MessageRouter and an extractor for FooMessage") {
        val messageRouter = MessageRouter()
        val fooExtractor = Extractor<FooMessage, Int>("fooExtractor") { message.v }

        val fooReceived = ConcurrentLinkedQueue<SenderMessage<FooMessage>>()

        context("Create listener for FooMessage(1) that will cancel after initial message is received") {
            println("A")
            messageRouter.listen(fooExtractor, 1, NEVER) {
                fooReceived += SenderMessage(sender, receivedMessage)
            }
            println("B")

            test("Listener should have been added to MessageRouter") {
                messageRouter.listeners[FooMessage::class]?.get("fooExtractor")?.size shouldBe 1
            }

            val sender = Peer(InetSocketAddress(InetAddress.getLocalHost(), 1234))

            test("Wrong message type should be ignored") {
                messageRouter.route(sender, BarMessage("ignored"))
                eventually(100.milliseconds) {
                    fooReceived.isEmpty() shouldBe true
                }
            }

            test("FooMessage(2) should be ignored") {
                messageRouter.route(sender, FooMessage(2, false))
                eventually(100.milliseconds) {
                    fooReceived.isEmpty() shouldBe true
                }
            }

            test("FooMessage(1) should be added to fooReceived") {
                messageRouter.route(sender, FooMessage(1, false))
                eventually(100.milliseconds) {
                    fooReceived shouldContainExactly listOf(SenderMessage(sender, FooMessage(1, false)))
                }
            }

        }
    }
})
