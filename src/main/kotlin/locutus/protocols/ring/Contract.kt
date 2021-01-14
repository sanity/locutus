package locutus.protocols.ring

import java.time.Instant

interface Contract<P : Post> {
    fun valid(p : P) : Boolean
}

interface Post {
    val id : UInt

    val published : Instant
}