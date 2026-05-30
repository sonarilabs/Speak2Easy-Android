package com.example.speak2easy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires the daily practice notification when the AlarmManager wakes us up. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderManager(context.applicationContext).fireReminderNotification()
    }
}
