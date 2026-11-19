package com.macrotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrotracker.data.local.entity.DailyLog
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.MacroPreset
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.local.relation.RecipeWithIngredients
import com.macrotracker.domain.MacroBreakdown
import com.macrotracker.domain.NutritionCalculator
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.theme.MacroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val MEAL_SLOTS = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    onOpenBarcodeScanner: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val todayWaterMl by viewModel.todayWaterMl.collectAsStateWithLifecycle()
    val activePreset by viewModel.activePreset.collectAsStateWithLifecycle()
    val macroPresets by viewModel.macroPresets.collectAsStateWithLifecycle()
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val scannedFood by viewModel.scannedFoodResult.collectAsStateWithLifecycle()

    var activeMealSlotSheet by remember { mutableStateOf<String?>(null) }
    var preselectedFoodForSheet by remember { mutableStateOf<FoodWithUnits?>(null) }

    LaunchedEffect(scannedFood) {
        if (scannedFood != null) {
            preselectedFoodForSheet = scannedFood
            if (activeMealSlotSheet == null) {
                activeMealSlotSheet = "Snacks"
            }
            viewModel.clearScannedFoodResult()
        }
    }

    val targetKcal = activePreset?.kcalTarget ?: 2200
    val targetProtein = activePreset?.proteinTarget ?: 160
    val targetCarbs = activePreset?.carbsTarget ?: 220
    val targetFat = activePreset?.fatTarget ?: 65
    val targetWater = activePreset?.waterTargetMl ?: 2500

    val totalKcal = dailyLogs.sumOf { it.cachedKcal }
    val totalProtein = dailyLogs.sumOf { it.cachedProtein }
    val totalCarbs = dailyLogs.sumOf { it.cachedCarbs }
    val totalFat = dailyLogs.sumOf { it.cachedFat }

    val isToday = selectedDate == LocalDate.now()
    val dateLabel = remember(selectedDate) {
        when (selectedDate) {
            LocalDate.now() -> "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
            LocalDate.now().minusDays(1) -> "Yesterday, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MacroColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.shiftDate(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = MacroColors.TextPrimary
                    )
                    if (!isToday) {
                        TextButton(onClick = { viewModel.copySelectedDateToToday() }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copy meals to Today")
                        }
                    }
                }
                IconButton(onClick = { viewModel.shiftDate(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
                }
            }
        }

        // Switchable Macro Presets Row
        if (macroPresets.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(macroPresets, key = { it.id }) { preset ->
                        FilterChip(
                            selected = preset.isActive,
                            onClick = { viewModel.selectPreset(preset.id) },
                            label = { Text("${preset.name} (${preset.kcalTarget} kcal)") }
                        )
                    }
                }
            }
        }

        // Macro Summary Dashboard Card
        item {
            MacroDashboardCard(
                totalKcal = totalKcal,
                targetKcal = targetKcal,
                totalProtein = totalProtein,
                targetProtein = targetProtein,
                totalCarbs = totalCarbs,
                targetCarbs = targetCarbs,
                totalFat = totalFat,
                targetFat = targetFat
            )
        }

        // Water Quick-Add Card
        item {
            WaterTrackerCard(
                currentMl = todayWaterMl,
                targetMl = targetWater,
                onAddWater = { viewModel.addWater(it) }
            )
        }

        // Meal Slots
        items(MEAL_SLOTS) { slot ->
            val slotLogs = dailyLogs.filter { it.mealSlot.equals(slot, ignoreCase = true) }
            MealSlotCard(
                mealSlot = slot,
                logs = slotLogs,
                onAddClick = {
                    preselectedFoodForSheet = null
                    activeMealSlotSheet = slot
                },
                onDeleteLog = { viewModel.deleteDailyLog(it) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // 2-Tap Quick-Log Bottom Sheet
    val currentSlot = activeMealSlotSheet
    if (currentSlot != null) {
        QuickLogBottomSheet(
            mealSlot = currentSlot,
            allFoods = allFoods,
            allRecipes = allRecipes,
            recentLogs = recentLogs,
            initialSelectedFood = preselectedFoodForSheet,
            onOpenBarcodeScanner = onOpenBarcodeScanner,
            onQuickReLog = { recent ->
                viewModel.reLogRecent(recent, currentSlot)
                activeMealSlotSheet = null
            },
            onLogFood = { foodWithUnits, unit, qty ->
                viewModel.logFoodItem(currentSlot, foodWithUnits, unit, qty)
                activeMealSlotSheet = null
            },
            onLogRecipe = { recipeId, unitMode, qty ->
                viewModel.logRecipeItem(currentSlot, recipeId, unitMode, qty)
                activeMealSlotSheet = null
            },
            onDismiss = {
                activeMealSlotSheet = null
                preselectedFoodForSheet = null
            }
        )
    }
}

@Composable
private fun MacroDashboardCard(
    totalKcal: Double,
    targetKcal: Int,
    totalProtein: Double,
    targetProtein: Int,
    totalCarbs: Double,
    targetCarbs: Int,
    totalFat: Double,
    targetFat: Int
) {
    val kcalProgress = (totalKcal / targetKcal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    val remaining = (targetKcal - totalKcal).roundToInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${totalKcal.roundToInt()} /$targetKcal kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MacroColors.TextPrimary
                    )
                    Text(
                        text = if (remaining >= 0) "$remaining kcal remaining" else "${-remaining} kcal over target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remaining >= 0) MacroColors.TextSecondary else MacroColors.FatCoral
                    )
                }
                Text(
                    text = "${(kcalProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MacroColors.ProteinTeal
                )
            }

            LinearProgressIndicator(
                progress = { kcalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MacroColors.ProteinTeal,
                trackColor = MacroColors.CardSurfaceElevated
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroBarColumn(
                    label = "Protein",
                    current = totalProtein,
                    target = targetProtein,
                    color = MacroColors.ProteinTeal,
                    modifier = Modifier.weight(1f)
                )
                MacroBarColumn(
                    label = "Carbs",
                    current = totalCarbs,
                    target = targetCarbs,
                    color = MacroColors.CarbsAmber,
                    modifier = Modifier.weight(1f)
                )
                MacroBarColumn(
                    label = "Fat",
                    current = totalFat,
                    target = targetFat,
                    color = MacroColors.FatCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroBarColumn(
    label: String,
    current: Double,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (current / target.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            text = "${current.roundToInt()}g / ${target}g",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MacroColors.TextPrimary
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MacroColors.CardSurfaceElevated
        )
    }
}

@Composable
private fun WaterTrackerCard(
    currentMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit
) {
    val progress = (currentMl.toFloat() / targetMl.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MacroColors.WaterSkyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Water: $currentMl / $targetMl ml",
                        style = MaterialTheme.typography.titleMedium,
                        color = MacroColors.TextPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAddWater(250) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+250ml", color = MacroColors.WaterSkyBlue)
                    }
                    Button(
                        onClick = { onAddWater(500) },
                        colors = ButtonDefaults.buttonColors(containerColor = MacroColors.WaterSkyBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+500ml", color = Color(0xFF082F49))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MacroColors.WaterSkyBlue,
                trackColor = MacroColors.CardSurfaceElevated
            )
        }
    }
}

@Composable
private fun MealSlotCard(
    mealSlot: String,
    logs: List<DailyLog>,
    onAddClick: () -> Unit,
    onDeleteLog: (DailyLog) -> Unit
) {
    val slotKcal = logs.sumOf { it.cachedKcal }.roundToInt()
    val slotProtein = logs.sumOf { it.cachedProtein }.roundToInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = mealSlot,
                        style = MaterialTheme.typography.titleMedium,
                        color = MacroColors.TextPrimary
                    )
                    Text(
                        text = "$slotKcal kcal  •  ${slotProtein}g P",
                        style = MaterialTheme.typography.labelMedium,
                        color = MacroColors.TextSecondary
                    )
                }
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to $mealSlot",
                        tint = MacroColors.ProteinTeal
                    )
                }
            }

            if (logs.isNotEmpty()) {
                HorizontalDivider(
                    color = MacroColors.BorderSubtle,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                logs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.itemNameSnapshot.ifBlank { "Logged Item" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MacroColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatQty(log.quantityLogged)}${log.unitNameSnapshot} (${log.computedGrams.roundToInt()}g)  •  P ${log.cachedProtein.roundToInt()}  C ${log.cachedCarbs.roundToInt()}  F${log.cachedFat.roundToInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MacroColors.TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${log.cachedKcal.roundToInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MacroColors.TextPrimary
                            )
                            IconButton(
                                onClick = { onDeleteLog(log) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MacroColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogBottomSheet(
    mealSlot: String,
    allFoods: List<FoodWithUnits>,
    allRecipes: List<RecipeWithIngredients>,
    recentLogs: List<DailyLog>,
    initialSelectedFood: FoodWithUnits?,
    onOpenBarcodeScanner: () -> Unit,
    onQuickReLog: (DailyLog) -> Unit,
    onLogFood: (FoodWithUnits, FoodUnit?, Double) -> Unit,
    onLogRecipe: (Long, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFood by remember(initialSelectedFood) { mutableStateOf(initialSelectedFood) }
    var selectedRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }

    var selectedUnitIndex by remember { mutableIntStateOf(0) }
    var selectedRecipeUnitMode by remember { mutableStateOf("SERVING") }
    var quantityText by remember { mutableStateOf("1") }

    val distinctRecents = remember(recentLogs) {
        recentLogs.distinctBy { "${it.foodItemId}_${it.recipeId}_${it.unitNameSnapshot}_${it.quantityLogged}" }
            .take(8)
    }

    val foodsMap = remember(allFoods) {
