package dev.ed3c.gymcometrue.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class PendingReminderStoreParsingTest {
    @Test
    fun parsesWellFormedEntries() {
        assertEquals(setOf(100L, 200L), parsePendingTriggers(setOf("100", "200")))
    }

    @Test
    fun dropsMalformedEntriesInsteadOfCrashing() {
        assertEquals(setOf(100L), parsePendingTriggers(setOf("100", "not-a-number", "")))
    }

    @Test
    fun emptyInputYieldsEmptyOutput() {
        assertEquals(emptySet(), parsePendingTriggers(emptySet()))
    }
}
