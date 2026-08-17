package dev.ed3c.gymcometrue.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

private const val immediateFireDelayMillis = 5_000L

/**
 * Reconciles persisted pending reminders after the three system transitions
 * this app can actually receive a broadcast for: reboot, this app's own
 * package being replaced (update), and a timezone change. All decision logic
 * lives in the pure [reconcilePendingTriggers]; this class only reads/writes
 * Android state around it. Never requests exact-alarm access — every re-arm
 * goes through the same inexact [ProtocolReminderScheduler.armAt] path.
 */
class ReminderTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ReminderTransitionEvent.BOOT_COMPLETED
            Intent.ACTION_MY_PACKAGE_REPLACED -> ReminderTransitionEvent.PACKAGE_REPLACED
            Intent.ACTION_TIMEZONE_CHANGED -> ReminderTransitionEvent.TIMEZONE_CHANGED
            else -> return
        }

        val now = System.currentTimeMillis()
        val pending = PendingReminderStore.load(context)
        val reconciliations = reconcilePendingTriggers(event, pending, now)

        val survivors = mutableSetOf<Long>()
        for (item in reconciliations) {
            when (item.disposition) {
                TriggerDisposition.REARM -> {
                    ProtocolReminderScheduler.armAt(context, item.triggerEpochMillis)
                    survivors += item.triggerEpochMillis
                }
                TriggerDisposition.ALREADY_ARMED_NO_ACTION -> survivors += item.triggerEpochMillis
                TriggerDisposition.FIRE_NOW -> {
                    val fireAt = now + immediateFireDelayMillis
                    ProtocolReminderScheduler.armAt(context, fireAt)
                    survivors += fireAt
                }
                TriggerDisposition.DROP_STALE -> Unit
            }
        }
        PendingReminderStore.save(context, survivors)
    }
}
