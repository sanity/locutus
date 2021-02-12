package locutus

import locutus.net.ConnectionManager
import locutus.net.UDPTransport
import locutus.protocols.bw.BandwidthManagementProtocol
import locutus.protocols.ring.PeerChooser
import locutus.protocols.ring.RingProtocol
import locutus.store.GatewayDAO
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.natpryce.konfig.*
import com.typesafe.config.ConfigFactory.systemProperties
import org.koin.core.scope.Scope
import java.time.Duration

val locutusModule = module {
    single { ConnectionManager(myKey = get(qualifier = named("myKey")), transport = get()) }

    single {
        val listenPort = cfg[Key("net.listen_port", intType)]
        val isOpen = cfg[Key("net.is_open", booleanType)]

        UDPTransport(
        port = listenPort,
        isOpen = isOpen
    ) }

    single { PeerChooser(ringProtocol = get(), bandwidthTracker = get(), bandwidthManagementProtocol = get()) }

    single { RingProtocol(connectionManager = get(), gateways = GatewayDAO.gatewayPeers()) }

    single {
        val updateEvery = cfg[Key("bandwidth_protocol.update_every_seconds", intType)].toLong()

        BandwidthManagementProtocol(
        cm = get(),
        bandwidthTracker = get(),
        ringProtocol = get(),
        updateEvery = Duration.ofSeconds(updateEvery)
    ) }

    single<Configuration> { ConfigurationProperties.fromResource("defaults.properties")  }

}

private val Scope.cfg get() = get<Configuration>()
