package locutus.protocols.ring.contracts

import kotlinx.serialization.KSerializer
import java.time.Instant

class Signed : Contract() {
    override fun valid(p: Post): Boolean {
        TODO("Not yet implemented")
    }

}

class SignedPost<D : Any>(override val id: Int, override val published: Instant, val serializer : KSerializer<D>, val data : ByteArray) : Post {

}