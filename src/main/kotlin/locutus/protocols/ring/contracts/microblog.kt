package locutus.protocols.ring.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import locutus.protocols.ring.store.GlobalStore
import locutus.tools.crypto.ec.ECSignature
import locutus.tools.crypto.ec.verify
import locutus.tools.crypto.rsa.RSASignature
import locutus.tools.crypto.rsa.verify
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

