@file:UseSerializers(RSAPublicKeySerializer::class)

package locutus.net.messages

import kotlinx.serialization.*
import kweb.util.random
import locutus.tools.crypto.rsa.RSAPublicKeySerializer
import locutus.tools.math.Location
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey

@Serializable
sealed class Message {

    val id = MessageId()

    abstract val respondingTo : MessageId?

    /**
     * Assimilation
     */

    @Serializable
    sealed class Assimilate : Message() {
        /*
         * Participants
         *
         * joiner: The peer that wants to join
         * gateway: An open peer through which the peer wishes to join, selects a location for the joiner
         *          and then forwards to peers in the direction of the location
         *
         */


        /**
         * Sent by joiner to gateway, requesting assimilation
         */
        @Serializable
        class GatewayRequest(val myPubKey : RSAPublicKey) : Assimilate() {

            override val respondingTo: MessageId? = null
        }

        @Serializable
        class GatewayAccept(override val respondingTo: MessageId, val yourAddress : Peer, val yourLocation : Location) : Assimilate()

        @Serializable
        class NewPeer(val joiner : Peer, val joinerPubKey : RSAPublicKey, val location: Location, val connectionsRequired : Int) : Assimilate() {
            init {
                require(connectionsRequired < 20)
            }


            override val respondingTo: MessageId? = null
        }

        @Serializable
        class NewPeerAccept(override val respondingTo: MessageId, val acceptorPeer: Peer, val acceptorPubKey : RSAPublicKey) : Assimilate()

    }

}

@Serializable
data class MessageId(val long : Long = random.nextLong())
