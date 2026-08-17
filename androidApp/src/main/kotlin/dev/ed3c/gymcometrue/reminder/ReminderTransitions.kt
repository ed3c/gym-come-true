package dev.ed3c.gymcometrue.reminder

/**
 * System transitions this app can actually receive a broadcast for and must
 * reconcile pending reminders against (Issue #31 / A2). Package data being
 * cleared has no broadcast an app can register for — an app whose data was
 * cleared cannot run code until it is next launched — so it is intentionally
 * absent from this set; see docs/android/reminder-reliability-harness.md for
 * that gap and its real-device evidence status.
 */
enum class ReminderTransitionEvent {
    BOOT_COMPLETED,
    PACKAGE_REPLACED,
    TIMEZONE_CHANGED,
}

/**
 * Whether the OS is expected to have dropped the alarm this app previously
 * armed with AlarmManager for this transition:
 *
 * - BOOT_COMPLETED always drops every AlarmManager alarm.
 * - PACKAGE_REPLACED drops alarms on some OEM builds but not stock AOSP;
 *   treated conservatively as "assume dropped" pending real-device evidence.
 * - TIMEZONE_CHANGED never drops an epoch-based ([java.lang.System.currentTimeMillis])
 *   alarm — only a wall-clock/local-time alarm would need one, and this app
 *   does not schedule those.
 */
private fun ReminderTransitionEvent.mayHaveDroppedArmedAlarm(): Boolean = when (this) {
    ReminderTransitionEvent.BOOT_COMPLETED -> true
    ReminderTransitionEvent.PACKAGE_REPLACED -> true
    ReminderTransitionEvent.TIMEZONE_CHANGED -> false
}

enum class TriggerDisposition {
    /** Still in the future; the OS is expected to still have it armed. */
    ALREADY_ARMED_NO_ACTION,

    /** Still in the future but must be re-armed because the OS likely dropped it. */
    REARM,

    /** Already due; fire it almost immediately instead of silently dropping it. */
    FIRE_NOW,

    /** Far enough in the past that firing it now would be confusing, not helpful. */
    DROP_STALE,
}

data class TriggerReconciliation(
    val triggerEpochMillis: Long,
    val disposition: TriggerDisposition,
)

/** Beyond this age, a missed trigger is dropped instead of fired late. */
const val DEFAULT_STALE_AFTER_MILLIS: Long = 24L * 60 * 60 * 1_000

/**
 * Deterministic reconciliation of persisted pending reminders against one
 * system transition. Pure: no Context, no AlarmManager, no I/O — exercised
 * directly by the reboot/timezone/package-transition test matrix instead of
 * a device or emulator.
 */
fun reconcilePendingTriggers(
    event: ReminderTransitionEvent,
    pendingTriggersEpochMillis: Set<Long>,
    nowEpochMillis: Long,
    staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
): List<TriggerReconciliation> {
    val rearmIfFuture = event.mayHaveDroppedArmedAlarm()
    return pendingTriggersEpochMillis.sorted().map { trigger ->
        val disposition = when {
            trigger > nowEpochMillis ->
                if (rearmIfFuture) TriggerDisposition.REARM else TriggerDisposition.ALREADY_ARMED_NO_ACTION
            nowEpochMillis - trigger > staleAfterMillis -> TriggerDisposition.DROP_STALE
            else -> TriggerDisposition.FIRE_NOW
        }
        TriggerReconciliation(trigger, disposition)
    }
}
