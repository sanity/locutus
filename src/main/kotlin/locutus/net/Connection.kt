package locutus.net

import locutus.net.messages.*
import locutus.tools.crypto.AESKey
import org.bouncycastle.jcajce.provider.symmetric.AES
import java.security.interfaces.RSAPublicKey
import java.time.Instant

class Connection(
    val peer: Peer,
    val pubKey: RSAPublicKey?,
    @Volatile var outboundKeyReceived: Boolean,
    val outboundKey : AESKey,
    val encryptedOutboundKeyPrefix : ByteArray,
    @Volatile var inboundKey : InboundKey?,
    @Volatile var lastKeepaliveReceived : Instant?
)

class InboundKey(val aesKey: AESKey, val inboundKeyPrefix: ByteArray?)