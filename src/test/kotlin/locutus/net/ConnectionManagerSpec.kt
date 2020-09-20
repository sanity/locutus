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
        val cm1 = ConnectionManager(13544, RSAKeyPair.create(), false)
        val cm2 = ConnectionManager(13545, RSAKeyPair.create(), true)
        val rp1 = Peer(InetSocketAddress(InetAddress.getLocalHost(), cm1.port))
        val rp2 = Peer(InetSocketAddress(InetAddress.getLocalHost(), cm2.port))
        val df1 = async {
            cm1.addConnection(rp1, cm1.myKey.public, false)
        }
        val df2 = async {
            cm2.addConnection(rp2, cm2.myKey.public, true)
        }
        df1.join()
        df2.join()
    }
})