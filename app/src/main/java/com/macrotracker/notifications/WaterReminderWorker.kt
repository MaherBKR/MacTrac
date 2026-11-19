package com.macrotracker.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.macrotracker.data.local.dao.DailyLogDao
import com.macrotracker.data.local.dao.HabitAndBackupDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dailyLogDao: DailyLogDao,
    private val habitAndBackupDao: HabitAndBackupDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentHour = LocalTime.now().hour
        // Pause hydration reminders during sleeping hours (10 PM - 8 AM)
        if (currentHour < 8 || currentHour >= 22) {
            return Result.success()
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val loggedMl = dailyLogDao.getWaterTotalForDate(today)
        val targetMl = habitAndBackupDao.getActivePreset()?.waterTargetMl ?: 2500

        // Automatically pause once the daily water goal is met
        if (loggedMl >= targetMl) {
            return Result.success()
        }

        val remaining = targetMl - loggedMl
        NotificationHelper.showReminderNotification(
            context = applicationContext,
            reminderId = 9001,
            title = "Hydration Check (${remaining}ml left)",
            type = "WATER"
        )

        return Result.success()
    }
}
