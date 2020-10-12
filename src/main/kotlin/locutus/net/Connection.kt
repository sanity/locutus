package locutus.net

import locutus.net.messages.*
import locutus.tools.crypto.AESKey
import org.bouncycastle.jcajce.provider.symmetric.AES
import java.security.interfaces.RSAPublicKey
import java.time.Instant

class Connection(
    val peer: Peer,
    val type : Type,
    @Volatile var lastKeepaliveReceived : Instant?
) {
    sealed class Type {
        abstract val decryptKey : AESKey?

        class Symmetric(
            val pubKey: RSAPublicKey,
            @Volatile var outboundKeyReceived: Boolean,
            val outboundKey : AESKey,
            val encryptedOutboundKeyPrefix : ByteArray,
            @Volatile var inboundKey : InboundKey?
        ) : Type() {
            override val decryptKey: AESKey?
                get() = inboundKey?.aesKey
        }

        class Outbound(
            val pubKey: RSAPublicKey,
            @Volatile var outboundKeyReceived: Boolean,
            val outboundKey : AESKey,
            val encryptedOutboundKeyPrefix : ByteArray,
        ) : Type() {
            override val decryptKey: AESKey
                get() = outboundKey

        }

        class Inbound(
            @Volatile var inboundKey : InboundKey
        ) : Type() {
            override val decryptKey: AESKey?
                get() = inboundKey.aesKey
        }
    }
}

class InboundKey(val aesKey: AESKey, val encryptedPrefix: ByteArray?)