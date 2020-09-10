package locutus.net.messages

import kotlinx.serialization.Serializable
import locutus.tools.crypto.RSASignature

@Serializable
sealed class Message {
    /**
     * Sent to establish an initial connection, this is the only message that doesn't indicate that the sender
     * has received the outbound synkey.
     */
    @Serializable
    data class Hello(val helloReceived : Boolean) : Message()

}

