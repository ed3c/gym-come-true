package dev.ed3c.gymcometrue.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderTransitionsTest {
    private val now = 1_000_000_000_000L
    private val oneHour = 60 * 60 * 1_000L

    @Test
    fun emptyPendingSetNeedsNoReconciliationOnAnyTransition() {
        for (event in ReminderTransitionEvent.entries) {
            assertEquals(emptyList(), reconcilePendingTriggers(event, emptySet(), now))
        }
    }

    @Test
    fun rebootRearmsAFutureTrigger() {
        val future = now + oneHour
        val result = reconcilePendingTriggers(ReminderTransitionEvent.BOOT_COMPLETED, setOf(future), now)
        assertEquals(listOf(TriggerReconciliation(future, TriggerDisposition.REARM)), result)
    }

    @Test
    fun packageReplaceRearmsAFutureTrigger() {
        val future = now + oneHour
        val result = reconcilePendingTriggers(ReminderTransitionEvent.PACKAGE_REPLACED, setOf(future), now)
        assertEquals(listOf(TriggerReconciliation(future, TriggerDisposition.REARM)), result)
    }

    @Test
    fun timezoneChangeLeavesAFutureEpochTriggerArmed() {
        val future = now + oneHour
        val result = reconcilePendingTriggers(ReminderTransitionEvent.TIMEZONE_CHANGED, setOf(future), now)
        assertEquals(listOf(TriggerReconciliation(future, TriggerDisposition.ALREADY_ARMED_NO_ACTION)), result)
    }

    @Test
    fun recentlyElapsedTriggerFiresNowInsteadOfSilentlyDropping() {
        val recentlyPast = now - (oneHour / 2)
        val result = reconcilePendingTriggers(ReminderTransitionEvent.BOOT_COMPLETED, setOf(recentlyPast), now)
        assertEquals(listOf(TriggerReconciliation(recentlyPast, TriggerDisposition.FIRE_NOW)), result)
    }

    @Test
    fun triggerOlderThanStaleWindowIsDroppedNotFired() {
        val ancient = now - DEFAULT_STALE_AFTER_MILLIS - oneHour
        val result = reconcilePendingTriggers(ReminderTransitionEvent.BOOT_COMPLETED, setOf(ancient), now)
        assertEquals(listOf(TriggerReconciliation(ancient, TriggerDisposition.DROP_STALE)), result)
    }

    @Test
    fun staleWindowBoundaryStillFiresRatherThanDrops() {
        val exactlyAtBoundary = now - DEFAULT_STALE_AFTER_MILLIS
        val result = reconcilePendingTriggers(ReminderTransitionEvent.BOOT_COMPLETED, setOf(exactlyAtBoundary), now)
        assertEquals(TriggerDisposition.FIRE_NOW, result.single().disposition)
    }

    @Test
    fun mixedBatchReconcilesEachTriggerIndependently() {
        val future = now + oneHour
        val recentlyPast = now - 1_000L
        val ancient = now - DEFAULT_STALE_AFTER_MILLIS - oneHour
        val result = reconcilePendingTriggers(
            ReminderTransitionEvent.BOOT_COMPLETED,
            setOf(future, recentlyPast, ancient),
            now,
        )
        assertEquals(
            setOf(
                TriggerReconciliation(future, TriggerDisposition.REARM),
                TriggerReconciliation(recentlyPast, TriggerDisposition.FIRE_NOW),
                TriggerReconciliation(ancient, TriggerDisposition.DROP_STALE),
            ),
            result.toSet(),
        )
    }
}
