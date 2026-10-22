package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered by AlarmManager to post a local recurring notification
 * reminding the user to back up their Tally Khata ledger to CSV.
 */
class BackupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = BackupReminderHelper.getSettings(context)
        if (settings.isEnabled) {
            BackupReminderHelper.showBackupNotification(context, isTest = false)
            // Schedule the next recurring reminder based on user's chosen frequency
            BackupReminderHelper.scheduleNextReminder(context, settings.frequencyDays)
        }
    }
}
