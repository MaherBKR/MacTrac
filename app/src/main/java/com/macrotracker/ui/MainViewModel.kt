package com.macrotracker.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macrotracker.data.backup.BackupManager
import com.macrotracker.data.local.entity.DailyLog
import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.MacroPreset
import com.macrotracker.data.local.entity.PantryItem
import com.macrotracker.data.local.entity.Recipe
import com.macrotracker.data.local.entity.RecipeIngredient
import com.macrotracker.data.local.entity.ReminderSchedule
import com.macrotracker.data.local.entity.ShoppingItem
import com.macrotracker.data.local.entity.WeightLog
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.repository.MacroTrackerRepository
import com.macrotracker.domain.NutritionCalculator
import com.macrotracker.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MacroTrackerRepository,
    private val backupManager: BackupManager,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _scannedFoodResult = MutableStateFlow<FoodWithUnits?>(null)
    val scannedFoodResult: StateFlow<FoodWithUnits?> = _scannedFoodResult.asStateFlow()

    val allFoods = repository.observeAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes = repository.observeAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyLogs = _selectedDate
        .flatMapLatest { date ->
            repository.observeLogsForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs = repository.observeRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterMl = _selectedDate
        .flatMapLatest { date ->
            repository.observeWaterTotalForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyMacroSummaries = _calendarMonth
        .flatMapLatest { ym ->
            val start = ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val end = ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.observeDailyMacroSummaries(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyWaterSummaries = _calendarMonth
        .flatMapLatest { ym ->
            val start = ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val end = ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.observeDailyWaterSummaries(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeightLogs = repository.observeAllWeightLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight = repository.observeLatestWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pantryItems = repository.observePantryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems = repository.observeShoppingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = repository.observeAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySpend = repository.observeWeeklySpend()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySpend = repository.observeMonthlySpend()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macroPresets = repository.observeAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePreset = repository.observeActivePreset()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminders = repository.observeAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Date & Calendar Navigation ---
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _calendarMonth.value = YearMonth.from(date)
    }

    fun shiftDate(days: Long) {
        selectDate(_selectedDate.value.plusDays(days))
    }

    fun shiftCalendarMonth(months: Long) {
        _calendarMonth.value = _calendarMonth.value.plusMonths(months)
    }

    // --- Quick-Log Food or Recipe ---
    fun logFoodItem(
        mealSlot: String,
        foodWithUnits: FoodWithUnits,
        unit: FoodUnit?,
        quantity: Double
    ) {
        if (quantity <= 0.0) return
        val macros = NutritionCalculator.calculateFoodMacros(foodWithUnits.food, unit, quantity)
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            repository.addDailyLog(
                DailyLog(
                    date = dateStr,
                    mealSlot = mealSlot,
                    foodItemId = foodWithUnits.food.id,
                    foodUnitId = unit?.id,
                    itemNameSnapshot = foodWithUnits.food.name,
                    unitNameSnapshot = unit?.unitName ?: foodWithUnits.food.defaultBaseUnit,
                    quantityLogged = quantity,
                    computedGrams = macros.grams,
                    cachedKcal = macros.kcal,
                    cachedProtein = macros.protein,
                    cachedCarbs = macros.carbs,
                    cachedFat = macros.fat
                )
            )
        }
    }

    fun logRecipeItem(
        mealSlot: String,
        recipeId: Long,
        unitMode: String, // "SERVING" or "GRAMS"
        quantity: Double
    ) {
        if (quantity <= 0.0) return
        val foodsMap = allFoods.value.associateBy { it.food.id }
        val recipesMap = allRecipes.value.associateBy { it.recipe.id }
        val recipe = recipesMap[recipeId]?.recipe ?: return
        val macros = NutritionCalculator.calculateRecipePortionMacros(
            recipeId = recipeId,
            unitMode = unitMode,
            quantity = quantity,
            allFoods = foodsMap,
            allRecipes = recipesMap
        )
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            repository.addDailyLog(
                DailyLog(
                    date = dateStr,
                    mealSlot = mealSlot,
                    recipeId = recipeId,
                    itemNameSnapshot = recipe.name,
                    unitNameSnapshot = if (unitMode == "GRAMS") "g" else "serving",
                    quantityLogged = quantity,
                    computedGrams = macros.grams,
                    cachedKcal = macros.kcal,
                    cachedProtein = macros.protein,
                    cachedCarbs = macros.carbs,
                    cachedFat = macros.fat
                )
            )
        }
    }

    fun reLogRecent(recent: DailyLog, mealSlot: String) {
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.addDailyLog(recent.copy(id = 0L, date = dateStr, mealSlot = mealSlot))
        }
    }

    fun deleteDailyLog(log: DailyLog) {
        viewModelScope.launch { repository.deleteDailyLog(log) }
    }

    fun copySelectedDateToToday() {
        val source = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (source == today) return
        viewModelScope.launch {
            repository.copyDateLogsToToday(source, today)
            _snackbarMessages.tryEmit("Copied meals to today")
        }
    }

    // --- Water & Weight ---
    fun addWater(amountMl: Int) {
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch { repository.logWater(dateStr, amountMl) }
    }

    fun addWeight(weightKg: Double, note: String = "") {
        if (weightKg <= 0.0) return
        val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repository.logWeight(dateStr, weightKg, note)
            _snackbarMessages.tryEmit("Weight logged: ${"%.1f".format(weightKg)} kg")
        }
    }

    fun deleteWeight(log: WeightLog) {
        viewModelScope.launch { repository.deleteWeightLog(log) }
    }

    // --- Foods, Barcode & Recipes ---
    fun saveFood(food: FoodItem, units: List<FoodUnit>) {
        viewModelScope.launch {
            repository.saveFoodWithUnits(food, units)
            _snackbarMessages.tryEmit("Saved ${food.name}")
        }
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch { repository.deleteFood(food) }
    }

    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            _snackbarMessages.tryEmit("Looking up barcode $barcode...")
            val result = repository.lookupOrFetchBarcode(barcode)
            if (result != null) {
                _scannedFoodResult.value = result
                _snackbarMessages.tryEmit("Found: ${result.food.name}")
            } else {
                _snackbarMessages.tryEmit("Barcode not found. You can add it manually.")
            }
        }
    }

    fun clearScannedFoodResult() {
        _scannedFoodResult.value = null
    }

    fun canAddSubRecipe(parentRecipeId: Long, candidateSubRecipeId: Long): Boolean {
        val recipesMap = allRecipes.value.associateBy { it.recipe.id }
        return !NutritionCalculator.wouldCreateCycle(parentRecipeId, candidateSubRecipeId, recipesMap)
    }

    fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        viewModelScope.launch {
            repository.saveRecipe(recipe, ingredients)
            _snackbarMessages.tryEmit("Saved recipe: ${recipe.name}")
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { repository.deleteRecipe(recipe) }
    }

    // --- Pantry (Decoupled) ---
    fun savePantryItem(item: PantryItem) {
        viewModelScope.launch {
            if (item.id == 0L) repository.savePantryItem(item) else repository.updatePantryItem(item)
        }
    }

    fun adjustPantryQuantity(item: PantryItem, delta: Double) {
        val updated = (item.quantity + delta).coerceAtLeast(0.0)
        viewModelScope.launch { repository.updatePantryItem(item.copy(quantity = updated)) }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch { repository.deletePantryItem(item) }
    }

    fun sendToShoppingList(item: PantryItem) {
        viewModelScope.launch {
            repository.sendPantryItemToShoppingList(item)
            _snackbarMessages.tryEmit("Added ${item.name} to Shopping List")
        }
    }

    // --- Shopping & Expenses ---
    fun saveShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            if (item.id == 0L) repository.saveShoppingItem(item) else repository.updateShoppingItem(item)
        }
    }

    fun toggleShoppingItemChecked(item: ShoppingItem) {
        viewModelScope.launch { repository.updateShoppingItem(item.copy(isChecked = !item.isChecked)) }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch { repository.deleteShoppingItem(item) }
    }

    fun completeShoppingTrip(
        storeOrNote: String,
        copyToPantry: Boolean,
        clearCheckedAfter: Boolean
    ) {
        val checked = shoppingItems.value.filter { it.isChecked }
        if (checked.isEmpty()) {
            _snackbarMessages.tryEmit("Check off at least one item first")
            return
        }
        viewModelScope.launch {
            repository.completeShoppingTrip(checked, storeOrNote, copyToPantry, clearCheckedAfter)
            _snackbarMessages.tryEmit("Shopping trip logged (${checked.size} items)")
        }
    }

    // --- Macro Presets & Reminders ---
    fun selectPreset(presetId: Long) {
        viewModelScope.launch { repository.selectActivePreset(presetId) }
    }

    fun savePreset(preset: MacroPreset) {
        viewModelScope.launch { repository.savePreset(preset) }
    }

    fun saveReminder(reminder: ReminderSchedule) {
        viewModelScope.launch {
            val savedId = repository.saveReminder(reminder)
            val bound = if (reminder.id == 0L) reminder.copy(id = savedId) else reminder
            reminderScheduler.scheduleReminder(bound)
            _snackbarMessages.tryEmit("Reminder saved")
        }
    }

    fun deleteReminder(reminder: ReminderSchedule) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminder)
            repository.deleteReminder(reminder)
        }
    }

    // --- Uninstall-Proof JSON Backup & Restore ---
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportToUri(uri)
                .onSuccess { _snackbarMessages.tryEmit("Backup exported successfully") }
                .onFailure { _snackbarMessages.tryEmit("Backup failed: ${it.localizedMessage}") }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.importFromUri(uri)
                .onSuccess { _snackbarMessages.tryEmit("Backup restored successfully") }
                .onFailure { _snackbarMessages.tryEmit("Restore failed: ${it.localizedMessage}") }
        }
    }
}
