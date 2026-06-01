package com.sonari.speak2easy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires the daily practice notification when the AlarmManager wakes us up, then re-arms the
 * alarm for the next day. We use one-shot `setExactAndAllowWhileIdle` (vs. inexact `setRepeating`)
 * so the alarm survives Doze, which means the receiver itself is responsible for the daily cadence.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mgr = ReminderManager(context.applicationContext)
        mgr.fireReminderNotification()
        val hour = intent.getIntExtra(ReminderManager.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(ReminderManager.EXTRA_MINUTE, -1)
        if (hour in 0..23 && minute in 0..59) {
            mgr.scheduleDailyReminder(hour, minute)
        }
    }
}
