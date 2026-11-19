package com.macrotracker.notifications

import android.Manifest
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
import com.macrotracker.MainActivity

object NotificationHelper {

    const val CHANNEL_MEALS = "macrotracker_meals"
    const val CHANNEL_WATER = "macrotracker_water"

    const val ACTION_SHOW_REMINDER = "com.macrotracker.ACTION_SHOW_REMINDER"
    const val ACTION_MARK_TAKEN = "com.macrotracker.ACTION_MARK_TAKEN"
    const val ACTION_LOG_WATER_250 = "com.macrotracker.ACTION_LOG_WATER_250"
    const val ACTION_SNOOZE_15 = "com.macrotracker.ACTION_SNOOZE_15"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TYPE = "extra_type"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mealsChannel = NotificationChannel(
            CHANNEL_MEALS,
            "Meals & Supplements",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Exact reminders for scheduled meals and supplements"
        }

        val waterChannel = NotificationChannel(
            CHANNEL_WATER,
            "Hydration Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Periodic water reminders during waking hours"
        }

        manager.createNotificationChannel(mealsChannel)
        manager.createNotificationChannel(waterChannel)
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Int,
        title: String,
        type: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (type.equals("WATER", ignoreCase = true)) CHANNEL_WATER else CHANNEL_MEALS
        val bodyText = when (type.uppercase()) {
            "WATER" -> "Stay hydrated! Tap below to log +250ml right away."
            "SUPPLEMENT" -> "Time to take your supplement."
            else -> "Time for your scheduled meal."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (type.equals("WATER", ignoreCase = true)) {
            val logWaterIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_LOG_WATER_250
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            val logWaterPending = PendingIntent.getBroadcast(
                context,
                reminderId * 10 + 1,
                logWaterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Log +250ml", logWaterPending)
        } else {
            val markTakenIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_MARK_TAKEN
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            val markTakenPending = PendingIntent.getBroadcast(
                context,
                reminderId * 10 + 2,
                markTakenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Mark Taken", markTakenPending)
        }

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_15
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TYPE, type)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            reminderId * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Snooze 15m", snoozePending)

        NotificationManagerCompat.from(context).notify(reminderId, builder.build())
    }
}
