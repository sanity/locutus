package locutus.protocols.ring.contracts

import java.time.Instant
import kotlin.reflect.KClass

abstract class Contract {

    abstract fun valid(p : Post) : Boolean
}

interface Post {
    val id : Int

    val published : Instant
}