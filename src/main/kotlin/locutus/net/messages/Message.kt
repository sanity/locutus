package locutus.net.messages

import kotlinx.serialization.Serializable
import locutus.tools.crypto.RSASignature

@Serializable sealed class Message {
    data class Handshake(val yourKeyReceived : Boolean, val synKeySignature : RSASignature) : Message()
}