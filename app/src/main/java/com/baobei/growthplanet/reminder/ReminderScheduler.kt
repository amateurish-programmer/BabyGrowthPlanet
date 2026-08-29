package com.baobei.growthplanet.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baobei.growthplanet.MainActivity
import com.baobei.growthplanet.R
import java.util.Calendar

object ReminderScheduler {
    private const val CHANNEL_ID = "growth_reminder"
    private const val REQUEST_CODE = 8020

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        createChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent(context))
    }

    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createChannel(context)
        val prefs = context.getSharedPreferences("growth_planet_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", false)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, "growth_reminder")
                .setSmallIcon(R.drawable.ic_star_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_text))
                .setAutoCancel(true).setContentIntent(open).build()
            NotificationManagerCompat.from(context).notify(8020, notification)
        }
        ReminderScheduler.schedule(context, prefs.getInt("reminder_hour", 20), prefs.getInt("reminder_minute", 0))
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val prefs = context.getSharedPreferences("growth_planet_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("reminder_enabled", false)) ReminderScheduler.schedule(context, prefs.getInt("reminder_hour", 20), prefs.getInt("reminder_minute", 0))
    }
}

