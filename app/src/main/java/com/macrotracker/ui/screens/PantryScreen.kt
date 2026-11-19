package com.macrotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrotracker.data.local.entity.PantryItem
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.theme.MacroColors
import kotlin.math.roundToInt

private val PANTRY_LOCATIONS = listOf("All", "Fridge", "Freezer", "Dry Pantry")

@Composable
fun PantryScreen(viewModel: MainViewModel) {
    val pantryItems by viewModel.pantryItems.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("All") }
    var editingItem by remember { mutableStateOf<PantryItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(pantryItems, selectedCategory) {
        if (selectedCategory == "All") pantryItems
        else pantryItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MacroColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showDialog = true
                },
                containerColor = MacroColors.ProteinTeal
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Pantry Item", tint = MacroColors.Background)
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
                text = "Pantry Inventory",
                style = MaterialTheme.typography.headlineSmall,
                color = MacroColors.TextPrimary
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PANTRY_LOCATIONS) { loc ->
                    FilterChip(
                        selected = selectedCategory == loc,
                        onClick = { selectedCategory = loc },
                        label = { Text(loc) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = "No items in $selectedCategory yet. Tap + to track your physical stock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MacroColors.TextSecondary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val isLow = item.quantity <= item.minThreshold
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MacroColors.TextPrimary
                                        )
                                        val meta = buildString {
                                            append(item.category)
                                            if (!item.expiryDate.isNullOrBlank()) {
                                                append(" • Exp: ${item.expiryDate}")
                                            }
                                            if (isLow) append(" • LOW STOCK")
                                        }
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isLow) MacroColors.WarningAmber else MacroColors.TextSecondary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.adjustPantryQuantity(item, -1.0) }) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        val qtyLabel = if (item.quantity % 1.0 == 0.0) {
                                            item.quantity.roundToInt().toString()
                                        } else {
                                            "%.1f".format(item.quantity)
                                        }
                                        Text(
                                            text = "$qtyLabel ${item.unitLabel}",
                                            fontWeight = FontWeight.Bold,
                                            color = MacroColors.TextPrimary
                                        )
                                        IconButton(onClick = { viewModel.adjustPantryQuantity(item, 1.0) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = MacroColors.ProteinTeal)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.sendToShoppingList(item) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Send to Shopping List")
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingItem = item
                                            showDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MacroColors.TextSecondary)
                                        }
                                        IconButton(onClick = { viewModel.deletePantryItem(item) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroColors.DangerRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        PantryItemDialog(
            initial = editingItem,
            onDismiss = { showDialog = false },
            onSave = { item ->
                viewModel.savePantryItem(item)
                showDialog = false
            }
        )
    }
}

@Composable
private fun PantryItemDialog(
    initial: PantryItem?,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var unitLabel by remember { mutableStateOf(initial?.unitLabel ?: "pcs") }
    var category by remember { mutableStateOf(initial?.category ?: "Dry Pantry") }
    var expiryDate by remember { mutableStateOf(initial?.expiryDate ?: "") }
    var minThreshold by remember { mutableStateOf(initial?.minThreshold?.toString() ?: "1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Pantry Item" else "Edit Pantry Item") },
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
                        label = { Text("Unit (pcs/kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Fridge", "Freezer", "Dry Pantry").forEach { loc ->
                        FilterChip(selected = category == loc, onClick = { category = loc }, label = { Text(loc) })
                    }
                }
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry YYYY-MM-DD (Optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = minThreshold,
                    onValueChange = { minThreshold = it },
                    label = { Text("Low-Stock Alert Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            PantryItem(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                foodItemId = initial?.foodItemId,
                                quantity = quantity.toDoubleOrNull() ?: 1.0,
                                unitLabel = unitLabel.trim().ifBlank { "pcs" },
                                category = category,
                                expiryDate = expiryDate.trim().takeIf { it.isNotBlank() },
                                minThreshold = minThreshold.toDoubleOrNull() ?: 1.0
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
