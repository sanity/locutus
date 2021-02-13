package locutus

import locutus.net.ConnectionManager
import locutus.net.UDPTransport
import locutus.protocols.bw.BandwidthManagementProtocol
import locutus.protocols.ring.PeerChooser
import locutus.protocols.ring.RingProtocol
import locutus.store.Gateway
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.serialization.builtins.serializer
import kweb.util.random
import locutus.store.CfgParam
import java.time.Duration


val locutusModule = module {
    single { ConnectionManager(myKey = get(qualifier = named("myKey")), transport = get()) }

    single {

        UDPTransport(
        port = CfgParam.computeIfAbsent("udp_listen_port", Int.serializer()) {
            random.nextInt(65535-49152) + 49152 },
        isOpen = CfgParam.computeIfAbsent("is_open", Boolean.serializer()) { false }
    ) }

    single { PeerChooser(ringProtocol = get(), bandwidthTracker = get(), bandwidthManagementProtocol = get()) }

    single { RingProtocol(connectionManager = get(), gateways = Gateway.gatewayPeers()) }

    single {
        val updateEvery = CfgParam.computeIfAbsent("bw_update_interval", Long.serializer()) { 60 }

        BandwidthManagementProtocol(
        cm = get(),
        bandwidthTracker = get(),
        ringProtocol = get(),
        updateEvery = Duration.ofSeconds(updateEvery)
    ) }


}
