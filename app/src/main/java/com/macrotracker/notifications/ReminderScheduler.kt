package com.macrotracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.macrotracker.data.local.entity.ReminderSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: ReminderSchedule) {
        if (!reminder.isActive) {
            cancelReminder(reminder)
            return
        }

        if (reminder.type.equals("WATER", ignoreCase = true)) {
            val interval = (reminder.intervalMinutes ?: 90).coerceAtLeast(15).toLong()
            val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                interval,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "water_reminder_${reminder.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            return
        }

        val parts = reminder.triggerTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = LocalDateTime.now()
        var target = now.with(LocalTime.of(hour, minute, 0))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        val triggerAtMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        scheduleExactAlarm(
            reminderId = reminder.id.toInt(),
            title = reminder.title,
            type = reminder.type,
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleSnooze(reminderId: Int, title: String, type: String, delayMinutes: Long = 15L) {
        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)
        scheduleExactAlarm(reminderId, title, type, triggerAtMillis)
    }

    private fun scheduleExactAlarm(
        reminderId: Int,
        title: String,
        type: String,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_SHOW_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationHelper.EXTRA_TITLE, title)
            putExtra(NotificationHelper.EXTRA_TYPE, type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminder: ReminderSchedule) {
        if (reminder.type.equals("WATER", ignoreCase = true)) {
            WorkManager.getInstance(context).cancelUniqueWork("water_reminder_${reminder.id}")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
