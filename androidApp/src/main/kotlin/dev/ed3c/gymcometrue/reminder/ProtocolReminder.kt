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

object ProtocolReminderScheduler {
    private const val channelId = "protocol-reminders"

    /**
     * Schedules an inexact local reminder. This deliberately avoids exact-alarm
     * special access and must not be presented as a guaranteed alarm clock.
     */
    fun schedule(context: Context, delayMillis: Long) {
        require(delayMillis >= 0L)
        val triggerAt = System.currentTimeMillis() + delayMillis
        val intent = Intent(context, ProtocolReminderReceiver::class.java).apply {
            putExtra("title", "Protocol checkpoint")
            putExtra("body", "Confirm evidence and readiness before the next protocol step.")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            triggerAt.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}

class ProtocolReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
            .setContentTitle(intent.getStringExtra("title") ?: "Protocol checkpoint")
            .setContentText(
                intent.getStringExtra("body")
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
