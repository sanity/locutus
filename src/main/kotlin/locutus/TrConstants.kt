package locutus

object TrConstants {
    const val PERSISTENCE_CACHE_SIZE = 25
    const val MAX_UDP_PACKET_SIZE = 1400 // old value was 1450
    const val UDP_CONN_INIT_INTERVAL_SECONDS = 2
    const val version = "0.1"
    const val DEFAULT_BAOS_SIZE = 2048
    const val DEFAULT_UDP_ACK_TIMEOUT_MS = 1000
    const val UDP_SHORT_MESSAGE_RETRY_ATTEMPTS = 3 // old value was 5
    const val UDP_KEEP_ALIVE_DURATION = 7
    const val PUB_PEER_CONCURRENT_ASSIMILATE = 3
    const val MAINTENANCE_HOPS_TO_LIVE = 8
    const val TOPOLOGY_MAINTENANCE_PEERS_TO_REPLACE = 3
    const val HOPS_TO_LIVE_RESET = 4
    const val WAIT_FROM_FORWARDING_SEC = 30
    const val TOPOLOGY_MAINTENANCE_PRIORITY = 5.0
    const val MICROBLOG_BROADCAST_PRIORITY = 6.0
    const val BROADCAST_INIT_PRIORITY = 0
    const val MAINTENANCE_FREQUENCY_MIN = 1
    const val MAX_MICROBLOGS_FOR_VIEWING = 300
    const val SHORTENED_PUBLIC_KEY_SIZE = 4
    const val ID_MAP_SIZE = 500
    const val CONTACT_PRIORITY_INCREASE = 5
    const val GUI_WIDTH_PX = 600
    const val GUI_HEIGHT_PX = 600
    const val POST_HGAP_PX = 50
    val separator = System.getProperty("file.separator")
    val MAIN_WINDOW_ARTWORK_PATH = "artwork$separator"
    val IDENTITY_STORE_TEST_FILE_PATH = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "temp-id-store.json"
    const val FOLLOWING = "FOLLOWING"
    const val OWN = "Own"
    val imageIconPath = MAIN_WINDOW_ARTWORK_PATH + "tahrir-logo_small.png"
    var add = true

    /**
     * Records constants to do with the microblog XML format.
     *
     * Example: <mb>
     * <txt>This is a microblog with a mention </txt>
     * <mtn alias="name">public key encoded in base 64</mtn>
    </mb> *
     */
    object FormatInfo {
        var ALIAS_ATTRIBUTE_INDEX = 0
        var ROOT = "bm"
        var PLAIN_TEXT = "txt"
        var MENTION = "mtn"
        var ALIAS_ATTRIBUTE = "alias"
    }
}