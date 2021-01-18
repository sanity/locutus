package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable

@Serializable
sealed class Message

@Serializable
class TextMessage(val text : String)