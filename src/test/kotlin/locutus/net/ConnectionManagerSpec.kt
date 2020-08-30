package locutus.net

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.async
import kotlinx.serialization.ExperimentalSerializationApi
import locutus.tools.crypto.RSAKeyPair
import java.net.*
import java.time.Duration

@ExperimentalSerializationApi
class ConnectionManagerSpec : FunSpec({
    test("two non-open managers connect") {
        val cm1 = ConnectionManager(13544, RSAKeyPair.create(), false)
        val cm2 = ConnectionManager(13544, RSAKeyPair.create(), false)
        val rp1 = RemotePeer(InetSocketAddress(InetAddress.getLocalHost(), cm1.port), cm1.myKey.public)
        val rp2 = RemotePeer(InetSocketAddress(InetAddress.getLocalHost(), cm2.port), cm2.myKey.public)
        val df1 = async {
            cm1.connect(rp1, Duration.ofMillis(500))
        }
        val df2 = async {
            cm2.connect(rp2, Duration.ofMillis(500))
        }
        df1.join()
        df2.join()
    }
})