package com.macrotracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.macrotracker.data.local.AppDatabase
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FullBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val foodItems: List<FoodItem> = emptyList(),
    val foodUnits: List<FoodUnit> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val recipeIngredients: List<RecipeIngredient> = emptyList(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val pantryItems: List<PantryItem> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val shoppingExpenses: List<ShoppingExpense> = emptyList(),
    val shoppingExpenseItems: List<ShoppingExpenseItem> = emptyList(),
    val waterLogs: List<WaterLog> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList(),
    val macroPresets: List<MacroPreset> = emptyList(),
    val reminderSchedules: List<ReminderSchedule> = emptyList()
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val json: Json
) {
    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dao = db.habitAndBackupDao()
            val payload = FullBackupPayload(
                foodItems = dao.dumpFoodItems(),
                foodUnits = dao.dumpFoodUnits(),
                recipes = dao.dumpRecipes(),
                recipeIngredients = dao.dumpRecipeIngredients(),
                dailyLogs = dao.dumpDailyLogs(),
                pantryItems = dao.dumpPantryItems(),
                shoppingItems = dao.dumpShoppingItems(),
                shoppingExpenses = dao.dumpShoppingExpenses(),
                shoppingExpenseItems = dao.dumpShoppingExpenseItems(),
                waterLogs = dao.dumpWaterLogs(),
                weightLogs = dao.dumpWeightLogs(),
                macroPresets = dao.dumpMacroPresets(),
                reminderSchedules = dao.dumpReminderSchedules()
            )
            val serialized = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(serialized.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open output stream for backup file.")
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("Unable to read backup file.")

            val payload = json.decodeFromString<FullBackupPayload>(rawJson)
            val dao = db.habitAndBackupDao()

            db.withTransaction {
                db.clearAllTables()
                // Insert parent tables first to satisfy foreign key constraints
                dao.restoreFoodItems(payload.foodItems)
                dao.restoreFoodUnits(payload.foodUnits)
                dao.restoreRecipes(payload.recipes)
                dao.restoreRecipeIngredients(payload.recipeIngredients)
                dao.restorePantryItems(payload.pantryItems)
                dao.restoreShoppingItems(payload.shoppingItems)
                dao.restoreShoppingExpenses(payload.shoppingExpenses)
                dao.restoreShoppingExpenseItems(payload.shoppingExpenseItems)
                dao.restoreDailyLogs(payload.dailyLogs)
                dao.restoreWaterLogs(payload.waterLogs)
                dao.restoreWeightLogs(payload.weightLogs)
                dao.restoreMacroPresets(payload.macroPresets)
                dao.restoreReminderSchedules(payload.reminderSchedules)
            }
        }
    }
}
