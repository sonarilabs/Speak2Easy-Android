package com.sonari.speak2easy.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sonari.speak2easy.MainActivity
import com.sonari.speak2easy.R
import java.util.Calendar

/**
 * Owns the daily-reminder notification channel and the AlarmManager scheduling. Named
 * `ReminderManager` to avoid colliding with `android.app.NotificationManager` — semantically
 * the iOS analog of `Services/NotificationManager.swift`.
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] (vs. inexact `setRepeating`) so the alarm fires
 * even when the device is in Doze. We declare `USE_EXACT_ALARM` in the manifest. The alarm is
 * one-shot — [ReminderReceiver] re-arms for the next day after firing.
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
        val pi = createOrUpdatePendingIntent(hour, minute)
        val firstFireAt = nextOccurrence(hour, minute)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstFireAt, pi)
            Log.d(TAG, "Scheduled exact alarm for $firstFireAt (h=$hour m=$minute)")
        } else {
            // Fallback for devices/users that revoked the special exact-alarm permission.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstFireAt, pi)
            Log.w(TAG, "Exact alarms not permitted; scheduled inexact for $firstFireAt")
        }
    }

    fun cancelReminder() {
        val pi = lookupPendingIntent() ?: return
        alarmManager.cancel(pi)
        pi.cancel()
        Log.d(TAG, "Cancelled reminder alarm")
    }

    fun fireReminderNotification() {
        if (!isOsPermissionGranted()) {
            Log.w(TAG, "Skipping notification — POST_NOTIFICATIONS not granted")
            return
        }
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            // Status-bar icon must be a single-color silhouette. The launcher mipmap renders
            // as a gray square on API 21+ because the system strips colors; `ic_notification`
            // is a proper transparent-bg vector. `setColor` tints the chip background only.
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF22D3EE.toInt())
            .setContentTitle("Time to practice Japanese")
            .setContentText("Two minutes is enough to keep your streak alive.")
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        sysNm.notify(REMINDER_ID, notif)
        Log.d(TAG, "Fired notification")
    }

    private fun createOrUpdatePendingIntent(hour: Int, minute: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        // FLAG_UPDATE_CURRENT guarantees creation, so the platform never returns null here.
        return PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )!!
    }

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
        private const val TAG = "ReminderManager"
        const val CHANNEL_ID = "practice_reminders"
        const val REMINDER_ID = 1001
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
    }
}
