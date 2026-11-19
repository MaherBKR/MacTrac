package com.macrotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitAndBackupDao {

    // --- Macro Presets ---
    @Query("SELECT * FROM macro_presets ORDER BY id ASC")
    fun observeAllPresets(): Flow<List<MacroPreset>>

    @Query("SELECT * FROM macro_presets WHERE isActive = 1 LIMIT 1")
    fun observeActivePreset(): Flow<MacroPreset?>

    @Query("SELECT * FROM macro_presets WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePreset(): MacroPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: MacroPreset): Long

    @Update
    suspend fun updatePreset(preset: MacroPreset)

    @Delete
    suspend fun deletePreset(preset: MacroPreset)

    @Query("UPDATE macro_presets SET isActive = 0")
    suspend fun deactivateAllPresets()

    @Query("UPDATE macro_presets SET isActive = 1 WHERE id = :presetId")
    suspend fun activatePreset(presetId: Long)

    @Transaction
    suspend fun selectActivePreset(presetId: Long) {
        deactivateAllPresets()
        activatePreset(presetId)
    }

    // --- Reminder Schedules ---
    @Query("SELECT * FROM reminder_schedules ORDER BY triggerTime ASC")
    fun observeAllReminders(): Flow<List<ReminderSchedule>>

    @Query("SELECT * FROM reminder_schedules WHERE isActive = 1")
    suspend fun getActiveReminders(): List<ReminderSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderSchedule): Long

    @Update
    suspend fun updateReminder(reminder: ReminderSchedule)

    @Delete
    suspend fun deleteReminder(reminder: ReminderSchedule)

    // --- Full Backup Snapshot Readers ---
    @Query("SELECT * FROM food_items") suspend fun dumpFoodItems(): List<FoodItem>
    @Query("SELECT * FROM food_units") suspend fun dumpFoodUnits(): List<FoodUnit>
    @Query("SELECT * FROM recipes") suspend fun dumpRecipes(): List<Recipe>
    @Query("SELECT * FROM recipe_ingredients") suspend fun dumpRecipeIngredients(): List<RecipeIngredient>
    @Query("SELECT * FROM daily_logs") suspend fun dumpDailyLogs(): List<DailyLog>
    @Query("SELECT * FROM pantry_items") suspend fun dumpPantryItems(): List<PantryItem>
    @Query("SELECT * FROM shopping_items") suspend fun dumpShoppingItems(): List<ShoppingItem>
    @Query("SELECT * FROM shopping_expenses") suspend fun dumpShoppingExpenses(): List<ShoppingExpense>
    @Query("SELECT * FROM shopping_expense_items") suspend fun dumpShoppingExpenseItems(): List<ShoppingExpenseItem>
    @Query("SELECT * FROM water_logs") suspend fun dumpWaterLogs(): List<WaterLog>
    @Query("SELECT * FROM weight_logs") suspend fun dumpWeightLogs(): List<WeightLog>
    @Query("SELECT * FROM macro_presets") suspend fun dumpMacroPresets(): List<MacroPreset>
    @Query("SELECT * FROM reminder_schedules") suspend fun dumpReminderSchedules(): List<ReminderSchedule>

    // --- Full Restore Writers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreFoodItems(items: List<FoodItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreFoodUnits(items: List<FoodUnit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRecipes(items: List<Recipe>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRecipeIngredients(items: List<RecipeIngredient>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreDailyLogs(items: List<DailyLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePantryItems(items: List<PantryItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreShoppingItems(items: List<ShoppingItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreShoppingExpenses(items: List<ShoppingExpense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreShoppingExpenseItems(items: List<ShoppingExpenseItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWaterLogs(items: List<WaterLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWeightLogs(items: List<WeightLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreMacroPresets(items: List<MacroPreset>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreReminderSchedules(items: List<ReminderSchedule>)
}
