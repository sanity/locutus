@file:UseSerializers(BigDecimalSerializer::class)

package locutus.protocols.karma

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.contracts.Contract
import locutus.protocols.ring.contracts.Value
import locutus.protocols.ring.contracts.ValueUpdate
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.ECSignature
import locutus.tools.crypto.ec.verify
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
import locutus.tools.serializers.BigDecimalSerializer
import locutus.tools.serializers.DurationSerializer
import locutus.tools.serializers.IntRangeSerializer
import java.math.BigDecimal
import java.math.BigInteger
import java.security.interfaces.ECPublicKey
import java.util.*

@Serializable
data class AccountContract(val publicKey : ECPublicKey, ) : Contract() {

    override fun valid(store: GlobalStore, p: Value): Result<Unit> {
        if (p is AccountValue) {
            var previousBalance : BigDecimal = BigDecimal.ZERO
            for ((index, entry) in p.entries.withIndex()) {
               if (entry.accountEntry.from == this) {

               } else if (entry.accountEntry.to == this) {

               } else {
                   return Result.failure(Throwable("This transaction isn't being sent or received from this contract"))
               }
            }
            return Result.success(Unit)
        } else {
            return Result.failure(Throwable("value is a ${p::class}, not an AccountValue"))
        }
    }

    override fun update(old: Value, update: ValueUpdate): Value? {
        TODO("Not yet implemented")
    }

    fun verify(entry : SignedAccountEntry)
            = publicKey.verify(entry.signature, entry.payload)

}

@Serializable
class AccountValue(val entries : ArrayList<SignedAccountEntry>) : Value()

@Serializable
class SignedAccountEntry(val signature : ECSignature, val payload : ByteArray) {
    val accountEntry: Transfer by lazy { ProtoBuf.decodeFromByteArray(Transfer.serializer(), payload) }
}

@Serializable
class Transfer(
    val from: AccountContract,
    val to: AccountContract,
    val amount: Long,
    val newFromBalance: Long,
    val newFromNumber: Int,
    val newToBalance: Long,
    val newToNumber: Int
)

/*

@Serializable
sealed class AccountEntry(open val number : Int, open val change : BigDecimal, open val newBalance: BigDecimal) {
    class In(number : Int, change : BigDecimal, newBalance : BigDecimal, val from : AccountContract, fromNumber : Int) : AccountEntry(number, change, newBalance)
    class Out(number : Int, change : BigDecimal, newBalance : BigDecimal, val to : AccountContract, toNumber : Int) : AccountEntry(number, change, newBalance)
}

 */