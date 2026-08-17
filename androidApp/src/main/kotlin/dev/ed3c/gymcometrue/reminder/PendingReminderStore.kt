package dev.ed3c.gymcometrue.reminder

import android.content.Context

private const val prefsName = "reminder_pending_triggers"
private const val triggersKey = "pending_trigger_epoch_millis"

/**
 * Parses persisted trigger strings, dropping anything malformed instead of
 * crashing. Pure; unit-tested directly with malformed input.
 */
internal fun parsePendingTriggers(raw: Set<String>): Set<Long> =
    raw.mapNotNull { it.toLongOrNull() }.toSet()

/**
 * The set of AlarmManager triggers this app believes are armed.
 * AlarmManager itself has no query API, so this is the source of truth
 * [ReminderTransitionReceiver] reconciles against reboot/timezone/package
 * transitions.
 */
internal object PendingReminderStore {
    fun load(context: Context): Set<Long> =
        parsePendingTriggers(prefs(context).getStringSet(triggersKey, emptySet()).orEmpty())

    fun save(context: Context, triggers: Set<Long>) {
        prefs(context).edit()
            .putStringSet(triggersKey, triggers.map(Long::toString).toSet())
            .apply()
    }

    fun add(context: Context, triggerEpochMillis: Long) {
        save(context, load(context) + triggerEpochMillis)
    }

    fun remove(context: Context, triggerEpochMillis: Long) {
        save(context, load(context) - triggerEpochMillis)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
}
