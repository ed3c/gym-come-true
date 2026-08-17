package dev.ed3c.gymcometrue.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.ed3c.gymcometrue.MainActivity

private const val extraTitle = "title"
private const val extraBody = "body"
private const val extraTriggerAtEpochMillis = "triggerAtEpochMillis"

object ProtocolReminderScheduler {
    private const val channelId = "protocol-reminders"

    /**
     * Schedules an inexact local reminder. This deliberately avoids exact-alarm
     * special access and must not be presented as a guaranteed alarm clock.
     */
    fun schedule(context: Context, delayMillis: Long) {
        require(delayMillis >= 0L)
        armAt(context, System.currentTimeMillis() + delayMillis)
    }

    /**
     * Arms (or re-arms, after a reboot/package-replace/timezone reconciliation
     * — see [ReminderTransitionReceiver]) a reminder for an absolute trigger
     * time and records it as pending so it can be reconciled later.
     */
    internal fun armAt(context: Context, triggerAtEpochMillis: Long) {
        val intent = Intent(context, ProtocolReminderReceiver::class.java).apply {
            putExtra(extraTitle, "Protocol checkpoint")
            putExtra(extraBody, "Confirm evidence and readiness before the next protocol step.")
            putExtra(extraTriggerAtEpochMillis, triggerAtEpochMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            triggerAtEpochMillis.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, pendingIntent)
        PendingReminderStore.add(context, triggerAtEpochMillis)
    }
}

class ProtocolReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val triggerAtEpochMillis = intent.getLongExtra(extraTriggerAtEpochMillis, -1L)
        if (triggerAtEpochMillis >= 0L) {
            PendingReminderStore.remove(context, triggerAtEpochMillis)
            DeliveryDelayLog.record(context, System.currentTimeMillis() - triggerAtEpochMillis)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Protocol reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Inexact reminders for user-confirmed fitness protocol checkpoints"
            },
        )

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(intent.getStringExtra(extraTitle) ?: "Protocol checkpoint")
            .setContentText(
                intent.getStringExtra(extraBody)
                    ?: "Review the next protocol step before acting.",
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(1_001, notification)
    }

    private companion object {
        const val channelId = "protocol-reminders"
    }
}
