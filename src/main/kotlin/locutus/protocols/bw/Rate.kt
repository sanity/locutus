package locutus.protocols.bw

import java.time.Duration
import java.time.Instant

class Rate(val windowSize : Duration = Duration.ofMinutes(1)) {
    private data class Sample(val bytes : Int, val time : Instant)

    private val window = java.util.LinkedList<Sample>()

    @Volatile
    private var totalBytes : Int = 0

    fun registerTraffic(bytes : Int, time : Instant = Instant.now()) {
        synchronized(this) {
            window.add(Sample(bytes, time))
            totalBytes += bytes
            trimWindow(time)
        }
    }

    fun rate(currentTime: Instant = Instant.now()) : BytesPerSecond {
        synchronized(this) {
            trimWindow()
            return totalBytes.toDouble() / (Duration.between(window.peek().time, currentTime).seconds)
        }
    }

    private fun trimWindow(currentTime : Instant = Instant.now()) {
        while (Duration.between(window.peek().time, currentTime) > windowSize) {
            val polled = window.poll()
            totalBytes -= polled.bytes
        }
    }
}