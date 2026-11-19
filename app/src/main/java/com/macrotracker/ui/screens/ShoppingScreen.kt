package com.macrotracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrotracker.data.local.entity.ShoppingItem
import com.macrotracker.data.local.relation.ExpenseWithItems
import com.macrotracker.data.local.relation.PeriodSpendSummary
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.theme.MacroColors

@Composable
fun ShoppingScreen(viewModel: MainViewModel) {
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val weeklySpend by viewModel.weeklySpend.collectAsStateWithLifecycle()
    val monthlySpend by viewModel.monthlySpend.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = List, 1 = Expenses
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val checkedTotal = shoppingItems.filter { it.isChecked }.sumOf { itemPrice(it) }
    val fullEstimatedTotal = shoppingItems.sumOf { itemPrice(it) }
    val checkedCount = shoppingItems.count { it.isChecked }

    Scaffold(
        containerColor = MacroColors.Background,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingItem = null
                        showItemDialog = true
                    },
                    containerColor = MacroColors.ProteinTeal
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Shopping Item", tint = MacroColors.Background)
                }
            }
        },
        bottomBar = {
            if (selectedTab == 0 && shoppingItems.isNotEmpty()) {
                Surface(
                    color = MacroColors.CardSurfaceElevated,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "In Cart ($checkedCount): ${"%.2f".format(checkedTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MacroColors.ProteinTeal
                            )
                            Text(
                                text = "Est. Full List: ${"%.2f".format(fullEstimatedTotal)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MacroColors.TextSecondary
                            )
                        }
                        Button(
                            onClick = { showCheckoutDialog = true },
                            enabled = checkedCount > 0
                        ) {
                            Text("Complete Trip")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Shopping & Budget",
                style = MaterialTheme.typography.headlineSmall,
                color = MacroColors.TextPrimary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Shopping List (${shoppingItems.size})", "Expenses (${allExpenses.size})").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        label = { Text(label) }
                    )
                }
            }

            if (selectedTab == 0) {
                if (shoppingItems.isEmpty()) {
                    Text(
                        text = "Your shopping list is empty. Tap + to add priced grocery items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MacroColors.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        items(shoppingItems, key = { it.id }) { item ->
                            val price = itemPrice(item)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { viewModel.toggleShoppingItemChecked(item) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (item.isChecked) MacroColors.TextSecondary else MacroColors.TextPrimary
                                        )
                                        Text(
                                            text = "${item.quantity} ${item.unitLabel} • ${item.category} • ${"%.2f".format(price)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MacroColors.TextSecondary
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingItem = item
                                        showItemDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MacroColors.TextSecondary)
                                    }
                                    IconButton(onClick = { viewModel.deleteShoppingItem(item) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroColors.DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ExpenseAnalyticsView(
                    weeklySpend = weeklySpend,
                    monthlySpend = monthlySpend,
                    allExpenses = allExpenses
                )
            }
        }
    }

    if (showItemDialog) {
        ShoppingItemDialog(
            initial = editingItem,
            onDismiss = { showItemDialog = false },
            onSave = { item ->
                viewModel.saveShoppingItem(item)
                showItemDialog = false
            }
        )
    }

    if (showCheckoutDialog) {
        CheckoutTripDialog(
            checkedCount = checkedCount,
            checkedTotal = checkedTotal,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { storeNote, copyToPantry, clearChecked ->
                viewModel.completeShoppingTrip(storeNote, copyToPantry, clearChecked)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
private fun ExpenseAnalyticsView(
    weeklySpend: List<PeriodSpendSummary>,
    monthlySpend: List<PeriodSpendSummary>,
    allExpenses: List<ExpenseWithItems>
) {
    var periodMode by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    val activeSummaries = if (periodMode == 0) weeklySpend.reversed() else monthlySpend.reversed()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Weekly Spend", "Monthly Spend").forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = periodMode == idx,
                                onClick = { periodMode = idx },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                                label = { Text(label) }
                            )
                        }
                    }

                    if (activeSummaries.isNotEmpty()) {
                        val latest = activeSummaries.last()
                        Text(
                            text = "Latest (${latest.periodKey}): ${"%.2f".format(latest.totalSpent)} (${latest.tripCount} trips)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MacroColors.CarbsAmber
                        )

                        val maxSpend = activeSummaries.maxOf { it.totalSpent }.coerceAtLeast(1.0)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(top = 6.dp)
                        ) {
                            val slotW = size.width / activeSummaries.size.coerceAtLeast(1)
                            val barW = (slotW * 0.55f).coerceAtLeast(10f)
                            activeSummaries.forEachIndexed { idx, summary ->
                                val barH = (summary.totalSpent / maxSpend).toFloat() * size.height
                                val x = idx * slotW + (slotW - barW) / 2f
                                drawRect(
                                    color = MacroColors.CarbsAmber,
                                    topLeft = Offset(x, size.height - barH),
                                    size = Size(barW, barH)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Complete a shopping trip to see your weekly and monthly grocery spending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MacroColors.TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Past Trip Receipts",
                style = MaterialTheme.typography.titleMedium,
                color = MacroColors.TextPrimary
            )
        }

        items(allExpenses, key = { it.expense.id }) { receipt ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = receipt.expense.storeOrNote.ifBlank { "Grocery Trip" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MacroColors.TextPrimary
                            )
                            Text(
                                text = "${receipt.expense.date} • ${receipt.expense.itemCount} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = MacroColors.TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "%.2f".format(receipt.expense.totalAmount),
                                fontWeight = FontWeight.Bold,
                                color = MacroColors.ProteinTeal
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MacroColors.TextSecondary
                            )
                        }
                    }

                    if (expanded && receipt.items.isNotEmpty()) {
                        HorizontalDivider(
                            color = MacroColors.BorderSubtle,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        receipt.items.forEach { line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${line.itemName} (${line.quantity} ${line.unitLabel})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MacroColors.TextPrimary
                                )
                                Text(
                                    text = "%.2f".format(line.pricePaid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MacroColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemDialog(
    initial: ShoppingItem?,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var unitLabel by remember { mutableStateOf(initial?.unitLabel ?: "pcs") }
    var unitPrice by remember { mutableStateOf(initial?.unitPrice?.takeIf { it > 0.0 }?.toString() ?: "") }
    var totalPrice by remember { mutableStateOf(initial?.totalPrice?.takeIf { it > 0.0 }?.toString() ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Groceries") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Shopping Item" else "Edit Shopping Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = { Text("Unit Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalPrice,
                        onValueChange = { totalPrice = it },
                        label = { Text("Or Total Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            ShoppingItem(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                quantity = quantity.toDoubleOrNull() ?: 1.0,
                                unitLabel = unitLabel.trim().ifBlank { "pcs" },
                                unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                                totalPrice = totalPrice.toDoubleOrNull() ?: 0.0,
                                category = category.trim().ifBlank { "Groceries" },
                                isChecked = initial?.isChecked ?: false,
                                pantryItemId = initial?.pantryItemId
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
private fun CheckoutTripDialog(
    checkedCount: Int,
    checkedTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit
) {
    var storeOrNote by remember { mutableStateOf("") }
    var copyToPantry by remember { mutableStateOf(true) }
    var clearChecked by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Trip & Log Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Logging $checkedCount checked items totaling ${"%.2f".format(checkedTotal)}.")
                OutlinedTextField(
                    value = storeOrNote,
                    onValueChange = { storeOrNote = it },
                    label = { Text("Store or Note (Optional)") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = copyToPantry, onCheckedChange = { copyToPantry = it })
                    Text("Copy checked items into Pantry")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearChecked, onCheckedChange = { clearChecked = it })
                    Text("Clear checked items from Shopping List")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(storeOrNote, copyToPantry, clearChecked) }) {
                Text("Log Expense")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun itemPrice(item: ShoppingItem): Double {
    return if (item.totalPrice > 0.0) item.totalPrice else item.unitPrice * item.quantity
}
