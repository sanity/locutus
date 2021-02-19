package locutus

import locutus.net.ConnectionManager
import locutus.net.UDPTransport
import locutus.protocols.bw.BandwidthManagementProtocol
import locutus.protocols.ring.PeerChooser
import locutus.protocols.ring.RingProtocol
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.serialization.builtins.serializer
import kweb.util.random
import locutus.state.*
import org.mapdb.DB
import java.time.Duration


val locutusModule = module {

    single<DB> {
        MapDB().db
    }

    single<ShoeboxFactory> { MapDBShoeboxFactory(get()) }

    single<CfgParams> { CfgParams(get()) }

    single<Gateways> { Gateways(get()) }

    single<ConnectionManager> { ConnectionManager(myKey = get(qualifier = named("myKey")), transport = get()) }

    single<UDPTransport> {

        val cfgParams : CfgParams = get()

        UDPTransport(
        port = cfgParams.computeIfAbsent("udp_listen_port", Int.serializer()) {
            random.nextInt(65535-49152) + 49152 },
        isOpen = cfgParams.computeIfAbsent("is_open", Boolean.serializer()) { false }
    ) }

    single<PeerChooser> { PeerChooser(ringProtocol = get(), bandwidthTracker = get(), bandwidthManagementProtocol = get()) }

    single<RingProtocol> {
        val gateways : Gateways = get()
        RingProtocol(connectionManager = get(), gateways = gateways.gatewayPeers())
    }

    single<BandwidthManagementProtocol> {
        val cfgParams : CfgParams = get()

        val updateEvery = cfgParams.computeIfAbsent("bw_update_interval", Long.serializer()) { 60 }

        BandwidthManagementProtocol(
        cm = get(),
        bandwidthTracker = get(),
        ringProtocol = get(),
        updateEvery = Duration.ofSeconds(updateEvery)
    ) }


}
