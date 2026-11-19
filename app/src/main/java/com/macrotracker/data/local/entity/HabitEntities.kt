package com.macrotracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "macro_presets")
data class MacroPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val kcalTarget: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
    val waterTargetMl: Int = 2500,
    val isActive: Boolean = false
)

@Serializable
@Entity(tableName = "reminder_schedules")
data class ReminderSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: String,               // "MEAL", "SUPPLEMENT", "WATER"
    val triggerTime: String,        // "HH:mm" (24-hour format)
    val intervalMinutes: Int? = null, // Used for repeating WATER reminders
    val repeatDays: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val isActive: Boolean = true
)
