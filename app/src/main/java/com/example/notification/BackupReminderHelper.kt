package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupReminderSettings(
    val isEnabled: Boolean,
    val frequencyDays: Int,
    val nextScheduledMillis: Long,
    val lastExportMillis: Long
)

object BackupReminderHelper {

    const val CHANNEL_ID = "ledger_csv_backup_reminders"
    const val CHANNEL_NAME = "Ledger Backup Reminders"
    const val NOTIFICATION_ID = 4001
    const val REMINDER_REQUEST_CODE = 4002

    const val ACTION_SEND_REMINDER = "com.example.tallykhata.ACTION_BACKUP_REMINDER"
    const val EXTRA_DESTINATION = "destination"
    const val DESTINATION_CSV_BACKUP = "csv_backup"

    private const val PREFS_NAME = "ledger_backup_reminders_prefs"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_FREQUENCY_DAYS = "frequency_days"
    private const val KEY_NEXT_SCHEDULED_MILLIS = "next_scheduled_millis"
    private const val KEY_LAST_EXPORT_MILLIS = "last_export_millis"

    const val FREQUENCY_DAILY = 1
    const val FREQUENCY_WEEKLY = 7
    const val FREQUENCY_BIWEEKLY = 14
    const val FREQUENCY_MONTHLY = 30

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Reminds business owners to regularly export ledger data as CSV for offline backups."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun getSettings(context: Context): BackupReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BackupReminderSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            frequencyDays = prefs.getInt(KEY_FREQUENCY_DAYS, FREQUENCY_WEEKLY),
            nextScheduledMillis = prefs.getLong(KEY_NEXT_SCHEDULED_MILLIS, 0L),
            lastExportMillis = prefs.getLong(KEY_LAST_EXPORT_MILLIS, 0L)
        )
    }

    fun saveSettings(context: Context, enabled: Boolean, frequencyDays: Int): BackupReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSettings = getSettings(context)

        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_FREQUENCY_DAYS, frequencyDays)
            .apply()

        if (enabled) {
            scheduleNextReminder(context, frequencyDays)
        } else {
            cancelReminder(context)
        }

        return getSettings(context)
    }

    fun scheduleNextReminder(context: Context, frequencyDaysOverride: Int? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        if (!enabled) return

        val freq = frequencyDaysOverride ?: prefs.getInt(KEY_FREQUENCY_DAYS, FREQUENCY_WEEKLY)
        val intervalMillis = freq.toLong() * 24L * 60L * 60L * 1000L
        val nextTriggerTime = System.currentTimeMillis() + intervalMillis

        prefs.edit()
            .putLong(KEY_NEXT_SCHEDULED_MILLIS, nextTriggerTime)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderReceiver::class.java).apply {
            action = ACTION_SEND_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_NEXT_SCHEDULED_MILLIS, 0L)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackupReminderReceiver::class.java).apply {
            action = ACTION_SEND_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun recordExportCompleted(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_EXPORT_MILLIS, now)
            .apply()

        // Reschedule next reminder from now
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        if (enabled) {
            scheduleNextReminder(context)
        }
    }

    fun showBackupNotification(context: Context, isTest: Boolean = false): Boolean {
        createNotificationChannel(context)

        // Android 13+ permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_CSV_BACKUP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTest) {
            "Test: CSV Backup Reminder"
        } else {
            "Time to Backup Your Ledger! (CSV)"
        }

        val shortText = if (isTest) {
            "Backup notifications are working! Tap to view CSV export."
        } else {
            "Export your latest customer balances and journal to CSV to keep your store's records safe."
        }

        val bigText = if (isTest) {
            "Notification test successful. Your recurring backup reminder will trigger according to your schedule. Tap below or open the app to export your ledger data anytime."
        } else {
            "Regular backups protect your business against data loss. Export your Parties directory and Transaction Journal as CSV spreadsheets to keep secure offline copies."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_backup_reminder)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_backup_reminder,
                "Export Now",
                contentPendingIntent
            )
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun formatFrequencyLabel(frequencyDays: Int): String {
        return when (frequencyDays) {
            FREQUENCY_DAILY -> "Daily"
            FREQUENCY_WEEKLY -> "Weekly (Recommended)"
            FREQUENCY_BIWEEKLY -> "Every 2 Weeks"
            FREQUENCY_MONTHLY -> "Monthly (30 Days)"
            else -> "Every $frequencyDays days"
        }
    }

    fun formatScheduledTime(millis: Long): String {
        if (millis <= 0L) return "Not scheduled"
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatLastExportTime(millis: Long): String {
        if (millis <= 0L) return "Never exported"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
