package locutus.net.messages

import kotlinx.serialization.Serializable

@Serializable sealed class Message {
    data class OpenConnection(val myVersion : Int) : Message()
}