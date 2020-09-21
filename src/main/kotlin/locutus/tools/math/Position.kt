package locutus.tools.math

import kotlinx.serialization.Serializable
import locutus.tools.crypto.hash
import java.security.interfaces.RSAPublicKey
import kotlin.math.abs

@Serializable
data class Position(val value : Double) {
    init {
        require(value in 0.0..1.0) {"$value must be between 0 and 1 inclusive"}
    }

    companion object {
        @ExperimentalUnsignedTypes
        fun fromByteArray(ba: UByteArray, precision: Int = 7) : Position {
            require(precision in 1..7) { "precision: $precision must be between 1 .. 7" }

            var value = 0.0
            var divisor : Long = 256
            ba.asSequence().take(precision).forEach { byte ->
                value += byte.toDouble() / divisor
                divisor *= 256
            }
            return Position(value)
        }

        @ExperimentalUnsignedTypes
        fun fromRSAPublicKey(pubKey : RSAPublicKey) : Position {
            return fromByteArray(pubKey.encoded.hash().toUByteArray())
        }
    }
}

infix fun Position.distance(other : Position) : Double {
    val d = abs(value - other.value)
    return if (d < 0.5) d else 1.0 - d
}
