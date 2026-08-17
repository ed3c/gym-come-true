package dev.ed3c.gymcometrue.reminder

import android.content.Context

private const val prefsName = "reminder_delivery_delays"
private const val delaysKey = "recent_delay_millis_csv"
internal const val DEFAULT_MAX_DELAY_ENTRIES = 30

/**
 * Order- and duplicate-preserving CSV parse. A bare `Set<String>` (as used by
 * [PendingReminderStore]) would silently collapse repeated delay values and
 * lose recency, which would bias [assessExactAlarmNeed]'s evidence.
 */
internal fun parseDelayCsv(raw: String?): List<Long> =
    raw.orEmpty()
        .split(",")
        .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toLongOrNull() }

/** Appends one delay sample, trimming to the most recent [maxEntries]. */
internal fun appendDelay(existing: List<Long>, newDelayMillis: Long, maxEntries: Int): List<Long> =
    (existing + newDelayMillis).takeLast(maxEntries)

/**
 * Rolling local log of (actual delivery time - intended trigger time) for
 * fired reminders. This is the only input [assessExactAlarmNeed] may use —
 * no other signal justifies requesting `SCHEDULE_EXACT_ALARM`.
 */
internal object DeliveryDelayLog {
    fun record(context: Context, delayMillis: Long, maxEntries: Int = DEFAULT_MAX_DELAY_ENTRIES) {
        val prefs = prefs(context)
        val updated = appendDelay(parseDelayCsv(prefs.getString(delaysKey, null)), delayMillis, maxEntries)
        prefs.edit().putString(delaysKey, updated.joinToString(",")).apply()
    }

    fun recent(context: Context): List<Long> = parseDelayCsv(prefs(context).getString(delaysKey, null))

    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
}
