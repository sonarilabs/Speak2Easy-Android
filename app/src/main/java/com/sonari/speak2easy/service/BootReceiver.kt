package com.sonari.speak2easy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sonari.speak2easy.data.prefs.SonariPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Re-arms the daily reminder after a device reboot — AlarmManager alarms don't survive otherwise.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appCtx = context.applicationContext
        val prefs = SonariPreferences(appCtx)
        val reminder = ReminderManager(appCtx)
        // BroadcastReceiver.onReceive must return quickly; reading two prefs is fast enough.
        runBlocking {
            val on = prefs.notificationsEnabled.first()
            if (!on || !reminder.isOsPermissionGranted()) return@runBlocking
            val minute = prefs.reminderMinuteOfDay.first()
            reminder.scheduleDailyReminder(minute / 60, minute % 60)
        }
    }
}
