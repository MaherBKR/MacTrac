package com.macrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.macrotracker.data.local.dao.DailyLogDao
import com.macrotracker.data.local.entity.WaterLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dailyLogDao: DailyLogDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(NotificationHelper.EXTRA_REMINDER_ID, 1000)
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "MacroTracker Reminder"
        val type = intent.getStringExtra(NotificationHelper.EXTRA_TYPE) ?: "MEAL"

        when (intent.action) {
            NotificationHelper.ACTION_SHOW_REMINDER -> {
                NotificationHelper.showReminderNotification(context, reminderId, title, type)
            }

            NotificationHelper.ACTION_MARK_TAKEN -> {
                NotificationManagerCompat.from(context).cancel(reminderId)
            }

            NotificationHelper.ACTION_LOG_WATER_250 -> {
                NotificationManagerCompat.from(context).cancel(reminderId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        dailyLogDao.insertWaterLog(WaterLog(date = today, amountMl = 250))
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            NotificationHelper.ACTION_SNOOZE_15 -> {
                NotificationManagerCompat.from(context).cancel(reminderId)
                reminderScheduler.scheduleSnooze(reminderId, title, type, delayMinutes = 15L)
            }
        }
    }
}
