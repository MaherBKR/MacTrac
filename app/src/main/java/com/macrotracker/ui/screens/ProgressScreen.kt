package com.macrotracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrotracker.data.local.entity.MacroPreset
import com.macrotracker.data.local.entity.ReminderSchedule
import com.macrotracker.data.local.entity.WeightLog
import com.macrotracker.data.local.relation.DailyMacroSummary
import com.macrotracker.data.local.relation.DailyWaterSummary
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.theme.MacroColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ProgressScreen(
    viewModel: MainViewModel,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onJumpToTodayTab: () -> Unit
) {
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val monthlyMacros by viewModel.monthlyMacroSummaries.collectAsStateWithLifecycle()
    val monthlyWater by viewModel.monthlyWaterSummaries.collectAsStateWithLifecycle()
    val allWeights by viewModel.allWeightLogs.collectAsStateWithLifecycle()
    val activePreset by viewModel.activePreset.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    var weightRange by remember { mutableStateOf("30D") }
    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    val targetKcal = activePreset?.kcalTarget ?: 2200
    val targetProtein = activePreset?.proteinTarget ?: 160
    val targetWater = activePreset?.waterTargetMl ?: 2500

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MacroColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Progress & Habits",
                style = MaterialTheme.typography.headlineSmall,
                color = MacroColors.TextPrimary
            )
        }

        // 1. Interactive Monthly Calendar
        item {
            MonthlyCalendarCard(
                yearMonth = calendarMonth,
                selectedDate = selectedDate,
                macroSummaries = monthlyMacros,
                waterSummaries = monthlyWater,
                targetKcal = targetKcal,
                targetProtein = targetProtein,
                targetWaterMl = targetWater,
                onPrevMonth = { viewModel.shiftCalendarMonth(-1) },
                onNextMonth = { viewModel.shiftCalendarMonth(1) },
                onSelectDate = { date ->
                    viewModel.selectDate(date)
                    onJumpToTodayTab()
                }
            )
        }

        // 2. Weight Trend (Raw + 7-Day Moving Average)
        item {
            WeightTrendCard(
                weights = allWeights,
                selectedRange = weightRange,
                onSelectRange = { weightRange = it },
                onLogWeightClick = { showLogWeightDialog = true }
            )
        }

        // 3. Water Trend Bar Chart + Streak
        item {
            WaterTrendCard(
                waterSummaries = monthlyWater,
                targetWaterMl = targetWater
            )
        }

        // 4. Macro Presets Quick Add
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Macro Targets & Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MacroColors.TextPrimary
                        )
                        Text(
                            text = "Active: ${activePreset?.name ?: "Default"} ($targetKcal kcal, ${targetProtein}g P)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MacroColors.TextSecondary
                        )
                    }
                    OutlinedButton(onClick = { showAddPresetDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Preset")
                    }
                }
            }
        }

        // 5. Actionable Reminders (Meals, Supplements, Water)
        item {
            RemindersCard(
                reminders = reminders,
                onAddClick = { showAddReminderDialog = true },
                onToggleActive = { reminder, active ->
                    viewModel.saveReminder(reminder.copy(isActive = active))
                },
                onDelete = { viewModel.deleteReminder(it) }
            )
        }

        // 6. Uninstall-Proof JSON Backup & Restore
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Uninstall-Proof Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        color = MacroColors.TextPrimary
                    )
                    Text(
                        text = "Export your entire database (foods, nested recipes, pantry, expenses, logs, and reminders) as a single .json file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MacroColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showLogWeightDialog) {
        LogWeightDialog(
            onDismiss = { showLogWeightDialog = false },
            onSave = { kg, note ->
                viewModel.addWeight(kg, note)
                showLogWeightDialog = false
            }
        )
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminder ->
                viewModel.saveReminder(reminder)
                showAddReminderDialog = false
            }
        )
    }

    if (showAddPresetDialog) {
        AddPresetDialog(
            onDismiss = { showAddPresetDialog = false },
            onSave = { preset ->
                viewModel.savePreset(preset)
                showAddPresetDialog = false
            }
        )
    }
}

@Composable
private fun MonthlyCalendarCard(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    macroSummaries: List<DailyMacroSummary>,
    waterSummaries: List<DailyWaterSummary>,
    targetKcal: Int,
    targetProtein: Int,
    targetWaterMl: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val macroMap = remember(macroSummaries) { macroSummaries.associateBy { it.date } }
    val waterMap = remember(waterSummaries) { waterSummaries.associateBy { it.date } }
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0 = Monday
    val daysInMonth = yearMonth.lengthOfMonth()

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Month")
                }
                Text(
                    text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MacroColors.TextPrimary
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MacroColors.TextSecondary
                    )
                }
            }

            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - startOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNumber)
                            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val macro = macroMap[key]
                            val water = waterMap[key]

                            val kcalHit = macro != null && abs(macro.totalKcal - targetKcal) <= targetKcal * 0.10
                            val proteinHit = macro != null && abs(macro.totalProtein - targetProtein) <= targetProtein * 0.10
                            val macroDotColor = when {
                                macro == null -> null
                                kcalHit && proteinHit -> MacroColors.SuccessGreen
                                else -> MacroColors.WarningAmber
                            }
                            val waterHit = (water?.totalMl ?: 0) >= targetWaterMl
                            val isSelected = date == selectedDate

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MacroColors.ProteinTeal else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectDate(date) }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MacroColors.TextPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.height(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (macroDotColor != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(macroDotColor)
                                        )
                                    }
                                    if (waterHit) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(MacroColors.WaterSkyBlue)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(MacroColors.SuccessGreen, "Target Hit")
                LegendDot(MacroColors.WarningAmber, "Logged")
                LegendDot(MacroColors.WaterSkyBlue, "Water Goal")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MacroColors.TextSecondary)
    }
}

@Composable
private fun WeightTrendCard(
    weights: List<WeightLog>,
    selectedRange: String,
    onSelectRange: (String) -> Unit,
    onLogWeightClick: () -> Unit
) {
    val filteredWeights = remember(weights, selectedRange) {
        val cutoff = when (selectedRange) {
            "7D" -> LocalDate.now().minusDays(7)
            "30D" -> LocalDate.now().minusDays(30)
            "90D" -> LocalDate.now().minusDays(90)
            else -> null
        }
        if (cutoff == null) weights
        else weights.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.isBefore(cutoff) == false }
    }

    val movingAvg = remember(filteredWeights) {
        filteredWeights.mapIndexed { idx, _ ->
            val start = (idx - 6).coerceAtLeast(0)
            val slice = filteredWeights.subList(start, idx + 1)
            slice.map { it.weightKg }.average()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weight Trend (7-Day Avg)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MacroColors.TextPrimary
                    )
                    val latest = weights.lastOrNull()?.weightKg
                    Text(
                        text = if (latest != null) "Latest: ${"%.1f".format(latest)} kg" else "No weigh-ins yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MacroColors.TextSecondary
                    )
                }
                Button(onClick = onLogWeightClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("+ Log Weight")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7D", "30D", "90D", "All").forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                        label = { Text(range) }
                    )
                }
            }

            if (filteredWeights.size >= 2) {
                val minW = (filteredWeights.minOf { it.weightKg } - 1.0)
                val maxW = (filteredWeights.maxOf { it.weightKg } + 1.0)
                val span = (maxW - minW).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val stepX = size.width / (filteredWeights.size - 1).coerceAtLeast(1)
                    val rawPath = Path()
                    val avgPath = Path()

                    filteredWeights.forEachIndexed { i, log ->
                        val x = i * stepX
                        val rawY = size.height - ((log.weightKg - minW) / span).toFloat() * size.height
                        val avgY = size.height - ((movingAvg[i] - minW) / span).toFloat() * size.height

                        if (i == 0) {
                            rawPath.moveTo(x, rawY)
                            avgPath.moveTo(x, avgY)
                        } else {
                            rawPath.lineTo(x, rawY)
                            avgPath.lineTo(x, avgY)
                        }
                        drawCircle(color = MacroColors.TextSecondary, radius = 4f, center = Offset(x, rawY))
                    }

                    drawPath(rawPath, color = MacroColors.TextSecondary.copy(alpha = 0.6f), style = Stroke(width = 3f))
                    drawPath(avgPath, color = MacroColors.ProteinTeal, style = Stroke(width = 6f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(MacroColors.TextSecondary, "Daily Weigh-In")
                    LegendDot(MacroColors.ProteinTeal, "7-Day Moving Avg")
                }
            } else {
                Text(
                    text = "Log at least 2 weigh-ins to see your trend line.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MacroColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WaterTrendCard(
    waterSummaries: List<DailyWaterSummary>,
    targetWaterMl: Int
) {
    val recent14 = waterSummaries.takeLast(14)
    val streak = remember(waterSummaries, targetWaterMl) {
        var count = 0
        for (entry in waterSummaries.reversed()) {
            if (entry.totalMl >= targetWaterMl) count++ else break
        }
        count
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Water Intake Trend",
                    style = MaterialTheme.typography.titleMedium,
                    color = MacroColors.TextPrimary
                )
                Text(
                    text = "Streak: $streak days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MacroColors.WaterSkyBlue
                )
            }

            if (recent14.isNotEmpty()) {
                val maxVal = maxOf(targetWaterMl, recent14.maxOf { it.totalMl }).coerceAtLeast(500)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val goalY = size.height - (targetWaterMl.toFloat() / maxVal) * size.height
                    drawLine(
                        color = MacroColors.WaterSkyBlue.copy(alpha = 0.7f),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    val slotWidth = size.width / recent14.size.coerceAtLeast(1)
                    val barWidth = (slotWidth * 0.6f).coerceAtLeast(6f)
                    recent14.forEachIndexed { idx, summary ->
                        val barHeight = (summary.totalMl.toFloat() / maxVal) * size.height
                        val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                        drawRect(
                            color = if (summary.totalMl >= targetWaterMl) MacroColors.WaterSkyBlue else MacroColors.WaterSkyBlue.copy(alpha = 0.4f),
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            } else {
                Text(
                    text = "Log water on the Today tab to track your daily hydration bars.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MacroColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RemindersCard(
    reminders: List<ReminderSchedule>,
    onAddClick: () -> Unit,
    onToggleActive: (ReminderSchedule, Boolean) -> Unit,
    onDelete: (ReminderSchedule) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MacroColors.CarbsAmber)
                    Spacer(Modifier.width(8.dp))
                    Text("Actionable Reminders", style = MaterialTheme.typography.titleMedium, color = MacroColors.TextPrimary)
                }
                OutlinedButton(onClick = onAddClick) {
                    Text("+ Add")
                }
            }

            if (reminders.isEmpty()) {
                Text(
                    text = "Schedule exact meal/supplement alarms or repeating hydration pings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MacroColors.TextSecondary
                )
            } else {
                reminders.forEach { reminder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reminder.title, color = MacroColors.TextPrimary, fontWeight = FontWeight.Medium)
                            val detail = if (reminder.type == "WATER") {
                                "Every ${reminder.intervalMinutes ?: 90} min (pauses at goal)"
                            } else {
                                "${reminder.type} at ${reminder.triggerTime}"
                            }
                            Text(detail, style = MaterialTheme.typography.labelMedium, color = MacroColors.TextSecondary)
                        }
                        Switch(
                            checked = reminder.isActive,
                            onCheckedChange = { onToggleActive(reminder, it) }
                        )
                        IconButton(onClick = { onDelete(reminder) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogWeightDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Body Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Optional Note") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kg = weightText.toDoubleOrNull()
                    if (kg != null && kg > 0.0) onSave(kg, noteText)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (ReminderSchedule) -> Unit
) {
    var title by remember { mutableStateOf("Creatine + Vitamin D") }
    var type by remember { mutableStateOf("SUPPLEMENT") }
    var timeText by remember { mutableStateOf("09:00") }
    var intervalText by remember { mutableStateOf("90") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MEAL", "SUPPLEMENT", "WATER").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true
                )
                if (type == "WATER") {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it },
                        label = { Text("Interval (minutes, min 15)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Time (HH:mm, 24h)") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            ReminderSchedule(
                                title = title.trim(),
                                type = type,
                                triggerTime = timeText.trim(),
                                intervalMinutes = if (type == "WATER") (intervalText.toIntOrNull() ?: 90) else null,
                                isActive = true
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddPresetDialog(
    onDismiss: () -> Unit,
    onSave: (MacroPreset) -> Unit
) {
    var name by remember { mutableStateOf("Cutting Day") }
    var kcal by remember { mutableStateOf("1900") }
    var protein by remember { mutableStateOf("170") }
    var carbs by remember { mutableStateOf("160") }
    var fat by remember { mutableStateOf("60") }
    var water by remember { mutableStateOf("2800") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Macro Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Preset Name") }, singleLine = true)
                OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("Calories (kcal)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = water, onValueChange = { water = it }, label = { Text("Water Target (ml)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            MacroPreset(
                                name = name.trim(),
                                kcalTarget = kcal.toIntOrNull() ?: 2000,
                                proteinTarget = protein.toIntOrNull() ?: 150,
                                carbsTarget = carbs.toIntOrNull() ?: 200,
                                fatTarget = fat.toIntOrNull() ?: 65,
                                waterTargetMl = water.toIntOrNull() ?: 2500,
                                isActive = false
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
