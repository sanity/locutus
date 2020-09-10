package locutus.net

import locutus.net.messages.*
import locutus.tools.crypto.AESKey
import org.bouncycastle.jcajce.provider.symmetric.AES
import java.security.interfaces.RSAPublicKey

class Connection(
    val peer: Peer,
    val outboundKeyReceived: Boolean,
    val outboundKey : AESKey,
    @Volatile var inboundKey : AESKey,
    @Volatile var inboundKeyPrefix: ByteArray?
)
