package locutus

import locutus.net.ConnectionManager
import locutus.net.UDPTransport
import locutus.protocols.ring.PeerChooser
import locutus.protocols.ring.RingProtocol
import locutus.store.GatewayDAO
import org.koin.core.qualifier.named
import org.koin.dsl.module

val locutusModule = module {
    single { ConnectionManager(myKey = get(qualifier = named("myKey")), transport = get()) }

    single { UDPTransport(
        port = get(qualifier = named("udpPort")),
        isOpen = get(qualifier = named("isOpen"))
    ) }

    single { PeerChooser(ringProtocol = get(), bandwidthTracker = get(), bandwidthManagementProtocol = get()) }

    single { RingProtocol(connectionManager = get(), gateways = GatewayDAO.gatewayPeers()) }

}