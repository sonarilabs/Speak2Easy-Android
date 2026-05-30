package com.example.speak2easy.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.speak2easy.MainActivity
import com.example.speak2easy.R
import java.util.Calendar

/**
 * Owns the daily-reminder notification channel and the AlarmManager scheduling. Named
 * `ReminderManager` to avoid colliding with `android.app.NotificationManager` — semantically
 * the iOS analog of `Services/NotificationManager.swift`.
 */
class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sysNm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannel()
    }

    fun isOsPermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val pi = createOrUpdatePendingIntent()
        val firstFireAt = nextOccurrence(hour, minute)
        // setRepeating is inexact in modern Android (≥ API 19), which is fine for a daily reminder.
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            firstFireAt,
            AlarmManager.INTERVAL_DAY,
            pi,
        )
    }

    fun cancelReminder() {
        val pi = lookupPendingIntent() ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun fireReminderNotification() {
        if (!isOsPermissionGranted()) return
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time to practice Japanese")
            .setContentText("Two minutes is enough to keep your streak alive.")
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        sysNm.notify(REMINDER_ID, notif)
    }

    // FLAG_UPDATE_CURRENT guarantees creation, so the platform never returns null here.
    private fun createOrUpdatePendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )!!

    // FLAG_NO_CREATE returns null when no matching PendingIntent exists; use for cancel.
    private fun lookupPendingIntent(): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Notification channels are idempotent — safe to re-create with the same id.
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Practice reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminder to practice your Japanese pronunciation."
        }
        sysNm.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_ID = "practice_reminders"
        const val REMINDER_ID = 1001
    }
}
