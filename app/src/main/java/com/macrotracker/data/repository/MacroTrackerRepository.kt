package com.macrotracker.data.repository

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
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.remote.OpenFoodFactsClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroTrackerRepository @Inject constructor(
    private val foodRecipeDao: FoodRecipeDao,
    private val dailyLogDao: DailyLogDao,
    private val pantryShoppingDao: PantryShoppingDao,
    private val habitAndBackupDao: HabitAndBackupDao,
    private val openFoodFactsClient: OpenFoodFactsClient
) {
    // --- Foods & Units ---
    fun observeAllFoods() = foodRecipeDao.observeAllFoodsWithUnits()
    fun searchFoods(query: String) = foodRecipeDao.searchFoodsWithUnits(query)

    suspend fun saveFoodWithUnits(food: FoodItem, units: List<FoodUnit>): Long {
        val foodId = if (food.id == 0L) {
            foodRecipeDao.insertFood(food)
        } else {
            foodRecipeDao.updateFood(food)
            foodRecipeDao.deleteUnitsForFood(food.id)
            food.id
        }
        val normalizedUnits = units.ifEmpty {
            listOf(FoodUnit(foodItemId = foodId, unitName = food.defaultBaseUnit, gramEquivalent = 1.0))
        }.map { it.copy(id = 0L, foodItemId = foodId) }

        foodRecipeDao.insertUnits(normalizedUnits)
        return foodId
    }

    suspend fun deleteFood(food: FoodItem) = foodRecipeDao.deleteFood(food)

    /**
     * Checks local Room DB first. If not found, queries Open Food Facts and caches it locally.
     */
    suspend fun lookupOrFetchBarcode(barcode: String): FoodWithUnits? {
        foodRecipeDao.getFoodByBarcode(barcode)?.let { return it }
        val remoteFood = openFoodFactsClient.fetchFoodByBarcode(barcode) ?: return null
        val savedId = saveFoodWithUnits(
            food = remoteFood,
            units = listOf(
                FoodUnit(foodItemId = 0L, unitName = "Gram", gramEquivalent = 1.0),
                FoodUnit(foodItemId = 0L, unitName = "Serving (100g)", gramEquivalent = 100.0)
            )
        )
        return foodRecipeDao.getFoodWithUnitsById(savedId)
    }

    // --- Recipes ---
    fun observeAllRecipes() = foodRecipeDao.observeAllRecipesWithIngredients()

    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>): Long {
        return foodRecipeDao.saveRecipeWithIngredients(recipe, ingredients)
    }

    suspend fun deleteRecipe(recipe: Recipe) = foodRecipeDao.deleteRecipe(recipe)

    // --- Daily Logs (Decoupled from Pantry) ---
    fun observeLogsForDate(date: String) = dailyLogDao.observeLogsForDate(date)
    fun observeRecentLogs() = dailyLogDao.observeRecentLogs()
    fun observeDailyMacroSummaries(startDate: String, endDate: String) =
        dailyLogDao.observeDailyMacroSummaries(startDate, endDate)

    suspend fun addDailyLog(log: DailyLog) = dailyLogDao.insertDailyLog(log)
    suspend fun updateDailyLog(log: DailyLog) = dailyLogDao.updateDailyLog(log)
    suspend fun deleteDailyLog(log: DailyLog) = dailyLogDao.deleteDailyLog(log)

    suspend fun copyDateLogsToToday(sourceDate: String, targetDate: String) {
        val logs = dailyLogDao.getLogsForDate(sourceDate)
        val copies = logs.map { it.copy(id = 0L, date = targetDate) }
        if (copies.isNotEmpty()) {
            dailyLogDao.insertDailyLogs(copies)
        }
    }

    // --- Water & Weight ---
    fun observeWaterTotalForDate(date: String) = dailyLogDao.observeWaterTotalForDate(date)
    fun observeDailyWaterSummaries(startDate: String, endDate: String) =
        dailyLogDao.observeDailyWaterSummaries(startDate, endDate)

    suspend fun logWater(date: String, amountMl: Int) {
        dailyLogDao.insertWaterLog(WaterLog(date = date, amountMl = amountMl))
    }

    fun observeAllWeightLogs() = dailyLogDao.observeAllWeightLogs()
    fun observeLatestWeight() = dailyLogDao.observeLatestWeight()

    suspend fun logWeight(date: String, weightKg: Double, note: String = "") {
        dailyLogDao.insertWeightLog(WeightLog(date = date, weightKg = weightKg, note = note))
    }

    suspend fun deleteWeightLog(log: WeightLog) = dailyLogDao.deleteWeightLog(log)

    // --- Standalone Pantry ---
    fun observePantryItems() = pantryShoppingDao.observeAllPantryItems()
    suspend fun savePantryItem(item: PantryItem) = pantryShoppingDao.insertPantryItem(item)
    suspend fun updatePantryItem(item: PantryItem) = pantryShoppingDao.updatePantryItem(item)
    suspend fun deletePantryItem(item: PantryItem) = pantryShoppingDao.deletePantryItem(item)

    suspend fun sendPantryItemToShoppingList(item: PantryItem) {
        pantryShoppingDao.insertShoppingItem(
            ShoppingItem(
                name = item.name,
                quantity = 1.0,
                unitLabel = item.unitLabel,
                category = item.category,
                pantryItemId = item.id
            )
        )
    }

    // --- Shopping List & Weekly/Monthly Expenses ---
    fun observeShoppingItems() = pantryShoppingDao.observeAllShoppingItems()
    fun observeAllExpenses() = pantryShoppingDao.observeAllExpensesWithItems()
    fun observeWeeklySpend() = pantryShoppingDao.observeWeeklySpendSummaries()
    fun observeMonthlySpend() = pantryShoppingDao.observeMonthlySpendSummaries()

    suspend fun saveShoppingItem(item: ShoppingItem) = pantryShoppingDao.insertShoppingItem(item)
    suspend fun updateShoppingItem(item: ShoppingItem) = pantryShoppingDao.updateShoppingItem(item)
    suspend fun deleteShoppingItem(item: ShoppingItem) = pantryShoppingDao.deleteShoppingItem(item)

    suspend fun completeShoppingTrip(
        checkedItems: List<ShoppingItem>,
        storeOrNote: String,
        copyToPantry: Boolean,
        clearCheckedAfter: Boolean
    ) {
        if (checkedItems.isEmpty()) return
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weekFields = WeekFields.ISO
        val weekNum = today.get(weekFields.weekOfWeekBasedYear())
        val weekYear = today.get(weekFields.weekBasedYear())
        val weekKey = String.format(Locale.US, "%04d-W%02d", weekYear, weekNum)
        val monthKey = String.format(Locale.US, "%04d-%02d", today.year, today.monthValue)

        val totalAmount = checkedItems.sumOf { item ->
            if (item.totalPrice > 0.0) item.totalPrice else item.unitPrice * item.quantity
        }

        val expense = ShoppingExpense(
            date = dateStr,
            weekKey = weekKey,
            monthKey = monthKey,
            totalAmount = totalAmount,
            itemCount = checkedItems.size,
            storeOrNote = storeOrNote.trim()
        )

        val expenseItems = checkedItems.map { item ->
            val paid = if (item.totalPrice > 0.0) item.totalPrice else item.unitPrice * item.quantity
            ShoppingExpenseItem(
                expenseId = 0L,
                itemName = item.name,
                category = item.category,
                quantity = item.quantity,
                unitLabel = item.unitLabel,
                pricePaid = paid
            )
        }

        pantryShoppingDao.recordExpenseWithItems(expense, expenseItems)

        if (copyToPantry) {
            checkedItems.forEach { item ->
                pantryShoppingDao.insertPantryItem(
                    PantryItem(
                        name = item.name,
                        quantity = item.quantity,
                        unitLabel = item.unitLabel,
                        category = item.category
                    )
                )
            }
        }

        if (clearCheckedAfter) {
            pantryShoppingDao.deleteCheckedShoppingItems()
        }
    }

    // --- Macro Presets & Reminders ---
    fun observeAllPresets() = habitAndBackupDao.observeAllPresets()
    fun observeActivePreset() = habitAndBackupDao.observeActivePreset()
    suspend fun selectActivePreset(presetId: Long) = habitAndBackupDao.selectActivePreset(presetId)
    suspend fun savePreset(preset: MacroPreset) = habitAndBackupDao.insertPreset(preset)
    suspend fun deletePreset(preset: MacroPreset) = habitAndBackupDao.deletePreset(preset)

    fun observeAllReminders() = habitAndBackupDao.observeAllReminders()
    suspend fun saveReminder(reminder: ReminderSchedule) = habitAndBackupDao.insertReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderSchedule) = habitAndBackupDao.deleteReminder(reminder)
}
