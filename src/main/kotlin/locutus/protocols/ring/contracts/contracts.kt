package locutus.protocols.ring.contracts

import kotlinx.serialization.Serializable
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.RSAPublicKey
import kotlin.reflect.KClass

@Serializable
sealed class Contract {
    abstract val postType : KClass<out Post>

    abstract fun valid(p : Post) : Boolean

    abstract fun supersedes(old : Post, new : Post) : Boolean
}

sealed class Post {

}
