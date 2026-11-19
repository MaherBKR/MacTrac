package com.macrotracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "daily_logs",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FoodUnit::class,
            parentColumns = ["id"],
            childColumns = ["foodUnitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["foodItemId"]),
        Index(value = ["recipeId"]),
        Index(value = ["foodUnitId"])
    ]
)
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,           // ISO format: YYYY-MM-DD
    val mealSlot: String,       // "Breakfast", "Lunch", "Dinner", "Snacks"
    val foodItemId: Long? = null,
    val recipeId: Long? = null,
    val foodUnitId: Long? = null,
    val itemNameSnapshot: String = "",
    val unitNameSnapshot: String = "g",
    val quantityLogged: Double,
    val computedGrams: Double,
    val cachedKcal: Double,
    val cachedProtein: Double,
    val cachedCarbs: Double,
    val cachedFat: Double
)

@Serializable
@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["date"])]
)
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,           // YYYY-MM-DD
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["date"])]
)
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,           // YYYY-MM-DD
    val weightKg: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
