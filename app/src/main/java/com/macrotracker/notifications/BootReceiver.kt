package com.macrotracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macrotracker.data.local.dao.HabitAndBackupDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitAndBackupDao: HabitAndBackupDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeReminders = habitAndBackupDao.getActiveReminders()
                activeReminders.forEach { reminderScheduler.scheduleReminder(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
