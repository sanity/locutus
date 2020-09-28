package locutus.net

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.async
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.net.messages.Peer
import locutus.tools.crypto.rsa.RSAKeyPair
import java.net.*

@ExperimentalSerializationApi
class ConnectionManagerSpec : FunSpec({
    test("two non-open managers connect") {
        TODO()
    }
})