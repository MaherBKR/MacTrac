package com.macrotracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.macrotracker.data.local.dao.DailyLogDao
import com.macrotracker.data.local.dao.FoodRecipeDao
import com.macrotracker.data.local.dao.HabitAndBackupDao
import com.macrotracker.data.local.dao.PantryShoppingDao
import com.macrotracker.data.local.entity.DailyLog
import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.MacroPreset
import com.macrotracker.data.local.entity.PantryItem
import com.macrotracker.data.local.entity.Recipe
import com.macrotracker.data.local.entity.RecipeIngredient
import com.macrotracker.data.local.entity.ReminderSchedule
import com.macrotracker.data.local.entity.ShoppingExpense
import com.macrotracker.data.local.entity.ShoppingExpenseItem
import com.macrotracker.data.local.entity.ShoppingItem
import com.macrotracker.data.local.entity.WaterLog
import com.macrotracker.data.local.entity.WeightLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SeedFoodDto(
    val name: String,
    val brand: String? = null,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultBaseUnit: String = "g",
    val units: List<SeedUnitDto>
)

@Serializable
private data class SeedUnitDto(
    val unitName: String,
    val gramEquivalent: Double
)

@Database(
    entities = [
        FoodItem::class,
        FoodUnit::class,
        Recipe::class,
        RecipeIngredient::class,
        DailyLog::class,
        PantryItem::class,
        ShoppingItem::class,
        ShoppingExpense::class,
        ShoppingExpenseItem::class,
        WaterLog::class,
        WeightLog::class,
        MacroPreset::class,
        ReminderSchedule::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodRecipeDao(): FoodRecipeDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun pantryShoppingDao(): PantryShoppingDao
    abstract fun habitAndBackupDao(): HabitAndBackupDao

    companion object {
        const val DATABASE_NAME = "macro_tracker.db"

        fun create(context: Context): AppDatabase {
            lateinit var instance: AppDatabase
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            val callback = object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    scope.launch {
                        seedInitialData(context.applicationContext, instance)
                    }
                }
            }

            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(callback)
                .build()

            return instance
        }

        private suspend fun seedInitialData(context: Context, db: AppDatabase) {
            val json = Json { ignoreUnknownKeys = true }
            val foodDao = db.foodRecipeDao()
            val habitDao = db.habitAndBackupDao()

            // Seed default Macro Presets
            habitDao.insertPreset(
                MacroPreset(
                    name = "Training Day",
                    kcalTarget = 2400,
                    proteinTarget = 180,
                    carbsTarget = 260,
                    fatTarget = 70,
                    waterTargetMl = 3000,
                    isActive = true
                )
            )
            habitDao.insertPreset(
                MacroPreset(
                    name = "Rest Day",
                    kcalTarget = 2000,
                    proteinTarget = 175,
                    carbsTarget = 175,
                    fatTarget = 65,
                    waterTargetMl = 2500,
                    isActive = false
                )
            )

            // Seed initial staple foods from assets/initial_foods.json
            runCatching {
                val rawJson = context.assets.open("initial_foods.json")
                    .bufferedReader()
                    .use { it.readText() }
                val seedFoods = json.decodeFromString<List<SeedFoodDto>>(rawJson)

                seedFoods.forEach { dto ->
                    val foodId = foodDao.insertFood(
                        FoodItem(
                            name = dto.name,
                            brand = dto.brand,
                            kcalPer100g = dto.kcalPer100g,
                            proteinPer100g = dto.proteinPer100g,
                            carbsPer100g = dto.carbsPer100g,
                            fatPer100g = dto.fatPer100g,
                            defaultBaseUnit = dto.defaultBaseUnit,
                            isBuiltIn = true
                        )
                    )
                    val units = dto.units.map { unitDto ->
                        FoodUnit(
                            foodItemId = foodId,
                            unitName = unitDto.unitName,
                            gramEquivalent = unitDto.gramEquivalent
                        )
                    }
                    foodDao.insertUnits(units)
                }
            }
        }
    }
}
