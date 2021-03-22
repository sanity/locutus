package locutus.tools

import kweb.shoebox.BinarySearchResult
import kweb.shoebox.betterBinarySearch
import java.util.Comparator

class PartTracker {
    private val received = ArrayList<IntRange>()

    operator fun plusAssign(part : Int) {
        require(part >= 0)
        synchronized(received) {
            for ((ix, range) in received.withIndex()) {
                when (part) {
                    range.first - 1 -> {
                        received[ix] = (part..range.last)
                        return
                    }
                    range.last + 1 -> {
                        received[ix] = (range.first..part)
                        return
                    }
                    in range -> {
                        return
                    }
                }
            }
            val newRange = part..part
            val pos = received.betterBinarySearch(newRange, Comparator.comparingInt { it.first })
            if (pos is BinarySearchResult.Between) {
                received.add(pos.highIndex, newRange)
            } else {
                error("Should never find exact match for newRange")
            }
        }
    }

    fun missingRanges(totalParts : Int) : ArrayList<IntRange> {
        val missing = ArrayList<IntRange>()
        synchronized(received) {
            if (received.isEmpty()) {
                return arrayListOf(0 until totalParts)
            }
            for (ix in received.indices) {
                val thisRange = received[ix]
                if (ix == 0) {
                    if (thisRange.first > 0) {
                        missing += 0 until thisRange.first
                    }
                }
                if (ix == received.indices.last) {
                    if (thisRange.last < totalParts - 1) {
                        missing += thisRange.last + 1 until totalParts
                    }
                } else {
                    val nextRange = received[ix + 1]
                    if (nextRange.first - thisRange.last > 0) {
                        missing += thisRange.last + 1 until nextRange.first
                    }
                }
            }
        }
        return missing
    }
}