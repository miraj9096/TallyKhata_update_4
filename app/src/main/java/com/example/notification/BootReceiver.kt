package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered when the device boots or when the application is updated,
 * ensuring recurring backup reminders are automatically rescheduled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val settings = BackupReminderHelper.getSettings(context)
            if (settings.isEnabled) {
                // If scheduled time has already passed while device was off, schedule immediately or from now
                val now = System.currentTimeMillis()
                if (settings.nextScheduledMillis > 0L && settings.nextScheduledMillis <= now) {
                    // Trigger notification or reschedule next period
                    BackupReminderHelper.showBackupNotification(context, isTest = false)
                }
                BackupReminderHelper.scheduleNextReminder(context, settings.frequencyDays)
            }
        }
    }
}
