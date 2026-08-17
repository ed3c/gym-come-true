package dev.ed3c.gymcometrue.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class DeliveryDelayLogParsingTest {
    @Test
    fun parsesOrderedCsvPreservingDuplicates() {
        assertEquals(listOf(100L, 100L, 200L), parseDelayCsv("100,100,200"))
    }

    @Test
    fun nullOrBlankInputYieldsEmptyList() {
        assertEquals(emptyList(), parseDelayCsv(null))
        assertEquals(emptyList(), parseDelayCsv(""))
    }

    @Test
    fun dropsMalformedEntriesInsteadOfCrashing() {
        assertEquals(listOf(100L, 200L), parseDelayCsv("100,oops,200"))
    }

    @Test
    fun appendTrimsToMaxEntriesKeepingMostRecent() {
        val existing = listOf(1L, 2L, 3L)
        assertEquals(listOf(2L, 3L, 4L), appendDelay(existing, 4L, maxEntries = 3))
    }
}
