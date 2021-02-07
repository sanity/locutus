package locutus

import locutus.net.ConnectionManager
import locutus.net.UDPTransport
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

val locutusModule = module {
    single { ConnectionManager(myKey = get(qualifier = named("myKey")), transport = get()) }

    single { UDPTransport(
        port = get(qualifier = named("udpPort")),
        isOpen = get(qualifier = named("isOpen"))
    ) }
}