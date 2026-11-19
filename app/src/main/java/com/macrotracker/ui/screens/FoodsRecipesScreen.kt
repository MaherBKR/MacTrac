package com.macrotracker.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.Recipe
import com.macrotracker.data.local.entity.RecipeIngredient
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.local.relation.RecipeWithIngredients
import com.macrotracker.domain.NutritionCalculator
import com.macrotracker.ui.MainViewModel
import com.macrotracker.ui.theme.MacroColors
import kotlin.math.roundToInt

@Composable
fun FoodsRecipesScreen(
    viewModel: MainViewModel,
    onOpenBarcodeScanner: () -> Unit
) {
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Foods, 1 = Recipes
    var searchQuery by remember { mutableStateOf("") }

    var editingFood by remember { mutableStateOf<FoodWithUnits?>(null) }
    var showFoodDialog by remember { mutableStateOf(false) }

    var editingRecipe by remember { mutableStateOf<RecipeWithIngredients?>(null) }
    var showRecipeDialog by remember { mutableStateOf(false) }

    val foodsMap = remember(allFoods) { allFoods.associateBy { it.food.id } }
    val recipesMap = remember(allRecipes) { allRecipes.associateBy { it.recipe.id } }

    Scaffold(
        containerColor = MacroColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingFood = null
                        showFoodDialog = true
                    } else {
                        editingRecipe = null
                        showRecipeDialog = true
                    }
                },
                containerColor = MacroColors.ProteinTeal
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = MacroColors.Background)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Foods & Recipes",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MacroColors.TextPrimary
                )
                IconButton(onClick = onOpenBarcodeScanner) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MacroColors.ProteinTeal
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Foods (${allFoods.size})", "Recipes (${allRecipes.size})").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (selectedTab == 0) "Search foods..." else "Search recipes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedTab == 0) {
                val filteredFoods = remember(allFoods, searchQuery) {
                    if (searchQuery.isBlank()) allFoods
                    else allFoods.filter {
                        it.food.name.contains(searchQuery, ignoreCase = true) ||
                            (it.food.brand?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredFoods, key = { it.food.id }) { foodWithUnits ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = foodWithUnits.food.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MacroColors.TextPrimary
                                    )
                                    Text(
                                        text = "Per 100${foodWithUnits.food.defaultBaseUnit}: ${foodWithUnits.food.kcalPer100g.roundToInt()} kcal • P ${foodWithUnits.food.proteinPer100g.roundToInt()}g • C ${foodWithUnits.food.carbsPer100g.roundToInt()}g • F ${foodWithUnits.food.fatPer100g.roundToInt()}g",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MacroColors.TextSecondary
                                    )
                                    if (foodWithUnits.units.isNotEmpty()) {
                                        Text(
                                            text = "Units: " + foodWithUnits.units.joinToString(", ") {
                                                "${it.unitName} (${it.gramEquivalent.roundToInt()}g)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MacroColors.ProteinTeal
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingFood = foodWithUnits
                                        showFoodDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MacroColors.TextSecondary)
                                    }
                                    IconButton(onClick = { viewModel.deleteFood(foodWithUnits.food) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroColors.DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val filteredRecipes = remember(allRecipes, searchQuery) {
                    if (searchQuery.isBlank()) allRecipes
                    else allRecipes.filter { it.recipe.name.contains(searchQuery, ignoreCase = true) }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRecipes, key = { it.recipe.id }) { recipeItem ->
                        val total = NutritionCalculator.calculateRecipeTotalMacros(
                            recipeItem.recipe.id, foodsMap, recipesMap
                        )
                        val perServing = NutritionCalculator.calculateRecipePortionMacros(
                            recipeItem.recipe.id, "SERVING", 1.0, foodsMap, recipesMap
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MacroColors.CardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recipeItem.recipe.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MacroColors.TextPrimary
                                    )
                                    Text(
                                        text = "Per serving: ${perServing.kcal.roundToInt()} kcal • P ${perServing.protein.roundToInt()}g • C ${perServing.carbs.roundToInt()}g • F ${perServing.fat.roundToInt()}g",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MacroColors.CarbsAmber
                                    )
                                    Text(
                                        text = "${recipeItem.ingredients.size} ingredients • ${recipeItem.recipe.totalServings} servings • Batch ${total.grams.roundToInt()}g",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MacroColors.TextSecondary
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingRecipe = recipeItem
                                        showRecipeDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MacroColors.TextSecondary)
                                    }
                                    IconButton(onClick = { viewModel.deleteRecipe(recipeItem.recipe) }) {
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

    if (showFoodDialog) {
        FoodEditorDialog(
            initial = editingFood,
            onDismiss = { showFoodDialog = false },
            onSave = { food, units ->
                viewModel.saveFood(food, units)
                showFoodDialog = false
            }
        )
    }

    if (showRecipeDialog) {
        RecipeEditorDialog(
            initial = editingRecipe,
            allFoods = allFoods,
            allRecipes = allRecipes,
            canAddSubRecipe = { parentId, subId -> viewModel.canAddSubRecipe(parentId, subId) },
            onDismiss = { showRecipeDialog = false },
            onSave = { recipe, ingredients ->
                viewModel.saveRecipe(recipe, ingredients)
                showRecipeDialog = false
            }
        )
    }
}

@Composable
private fun FoodEditorDialog(
    initial: FoodWithUnits?,
    onDismiss: () -> Unit,
    onSave: (FoodItem, List<FoodUnit>) -> Unit
) {
    var name by remember { mutableStateOf(initial?.food?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.food?.brand ?: "") }
    var kcal by remember { mutableStateOf(initial?.food?.kcalPer100g?.toString() ?: "") }
    var protein by remember { mutableStateOf(initial?.food?.proteinPer100g?.toString() ?: "") }
    var carbs by remember { mutableStateOf(initial?.food?.carbsPer100g?.toString() ?: "") }
    var fat by remember { mutableStateOf(initial?.food?.fatPer100g?.toString() ?: "") }
    var baseUnit by remember { mutableStateOf(initial?.food?.defaultBaseUnit ?: "g") }

    val units = remember {
        mutableStateListOf<FoodUnit>().apply {
            if (initial != null && initial.units.isNotEmpty()) {
                addAll(initial.units)
            } else {
                add(FoodUnit(foodItemId = 0L, unitName = "Gram", gramEquivalent = 1.0))
            }
        }
    }

    var newUnitName by remember { mutableStateOf("") }
    var newUnitGrams by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Food" else "Edit Food") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, singleLine = true)
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (Optional)") }, singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { kcal = it },
                        label = { Text("kcal / 100$baseUnit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = baseUnit == "g", onClick = { baseUnit = "g" }, label = { Text("Per 100g") })
                    FilterChip(selected = baseUnit == "ml", onClick = { baseUnit = "ml" }, label = { Text("Per 100ml") })
                }

                HorizontalDivider(color = MacroColors.BorderSubtle)
                Text("Equivalent Measuring Units", style = MaterialTheme.typography.titleMedium)

                units.forEachIndexed { idx, unit ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${unit.unitName} = ${unit.gramEquivalent}g", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { if (units.size > 1) units.removeAt(idx) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newUnitName,
                        onValueChange = { newUnitName = it },
                        label = { Text("Unit (e.g. Scoop)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = newUnitGrams,
                        onValueChange = { newUnitGrams = it },
                        label = { Text("Grams") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        val g = newUnitGrams.toDoubleOrNull()
                        if (newUnitName.isNotBlank() && g != null && g > 0.0) {
                            units.add(FoodUnit(foodItemId = 0L, unitName = newUnitName.trim(), gramEquivalent = g))
                            newUnitName = ""
                            newUnitGrams = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Equivalent Unit")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val food = FoodItem(
                            id = initial?.food?.id ?: 0L,
                            name = name.trim(),
                            brand = brand.trim().takeIf { it.isNotBlank() },
                            barcode = initial?.food?.barcode,
                            kcalPer100g = kcal.toDoubleOrNull() ?: 0.0,
                            proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                            carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                            fatPer100g = fat.toDoubleOrNull() ?: 0.0,
                            defaultBaseUnit = baseUnit,
                            isBuiltIn = initial?.food?.isBuiltIn ?: false
                        )
                        onSave(food, units.toList())
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RecipeEditorDialog(
    initial: RecipeWithIngredients?,
    allFoods: List<FoodWithUnits>,
    allRecipes: List<RecipeWithIngredients>,
    canAddSubRecipe: (Long, Long) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (Recipe, List<RecipeIngredient>) -> Unit
) {
    val parentId = initial?.recipe?.id ?: 0L
    var name by remember { mutableStateOf(initial?.recipe?.name ?: "") }
    var servings by remember { mutableStateOf(initial?.recipe?.totalServings?.toString() ?: "2") }
    var cookedWeight by remember { mutableStateOf(initial?.recipe?.totalCookedWeightGrams?.toString() ?: "") }
    var instructions by remember { mutableStateOf(initial?.recipe?.instructions ?: "") }

    val ingredients = remember {
        mutableStateListOf<RecipeIngredient>().apply {
            if (initial != null) addAll(initial.ingredients)
        }
    }

    var addMode by remember { mutableStateOf("FOOD") } // "FOOD" or "SUBRECIPE"
    var pickerQuery by remember { mutableStateOf("") }
    var ingredientQty by remember { mutableStateOf("100") }
    var subRecipeUnitMode by remember { mutableStateOf("SERVING") }

    val foodsMap = remember(allFoods) { allFoods.associateBy { it.food.id } }
    val recipesMap = remember(allRecipes) { allRecipes.associateBy { it.recipe.id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Recipe" else "Edit Recipe") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Recipe Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cookedWeight,
                        onValueChange = { cookedWeight = it },
                        label = { Text("Cooked g (Opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MacroColors.BorderSubtle)
                Text("Ingredients (${ingredients.size})", style = MaterialTheme.typography.titleMedium)

                ingredients.forEachIndexed { idx, ing ->
                    val label = if (ing.foodItemId != null) {
                        val f = foodsMap[ing.foodItemId]
                        val u = f?.units?.find { it.id == ing.foodUnitId }?.unitName ?: "g"
                        "${f?.food?.name ?: "Food"} • ${ing.quantity} $u"
                    } else {
                        val r = recipesMap[ing.subRecipeId]?.recipe?.name ?: "Sub-Recipe"
                        val u = if (ing.subRecipeUnitMode == "GRAMS") "g" else "serving"
                        "$r (Sub) • ${ing.quantity} $u"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { ingredients.removeAt(idx) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(color = MacroColors.BorderSubtle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = addMode == "FOOD",
                        onClick = { addMode = "FOOD"; ingredientQty = "100" },
                        label = { Text("+ Raw Food") }
                    )
                    FilterChip(
                        selected = addMode == "SUBRECIPE",
                        onClick = { addMode = "SUBRECIPE"; ingredientQty = "1" },
                        label = { Text("+ Sub-Recipe") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ingredientQty,
                        onValueChange = { ingredientQty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        label = { Text("Filter...") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                if (addMode == "SUBRECIPE") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = subRecipeUnitMode == "SERVING",
                            onClick = { subRecipeUnitMode = "SERVING" },
                            label = { Text("By Serving") }
                        )
                        FilterChip(
                            selected = subRecipeUnitMode == "GRAMS",
                            onClick = { subRecipeUnitMode = "GRAMS" },
                            label = { Text("By Grams") }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (addMode == "FOOD") {
                        val matches = allFoods.filter {
                            pickerQuery.isBlank() || it.food.name.contains(pickerQuery, ignoreCase = true)
                        }.take(8)
                        matches.forEach { f ->
                            val defaultUnit = f.units.firstOrNull { it.gramEquivalent == 1.0 } ?: f.units.firstOrNull()
                            Text(
                                text = "+ ${f.food.name} (${defaultUnit?.unitName ?: "g"})",
                                color = MacroColors.ProteinTeal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val q = ingredientQty.toDoubleOrNull() ?: 100.0
                                        if (q > 0.0) {
                                            ingredients.add(
                                                RecipeIngredient(
                                                    parentRecipeId = parentId,
                                                    foodItemId = f.food.id,
                                                    foodUnitId = defaultUnit?.id,
                                                    quantity = q
                                                )
                                            )
                                        }
                                    }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    } else {
                        val safeSubRecipes = allRecipes.filter { candidate ->
                            candidate.recipe.id != parentId &&
                                canAddSubRecipe(parentId, candidate.recipe.id) &&
                                (pickerQuery.isBlank() || candidate.recipe.name.contains(pickerQuery, ignoreCase = true))
                        }.take(8)
                        if (safeSubRecipes.isEmpty()) {
                            Text("No eligible sub-recipes available.", style = MaterialTheme.typography.labelMedium, color = MacroColors.TextSecondary)
                        } else {
                            safeSubRecipes.forEach { r ->
                                Text(
                                    text = "+ ${r.recipe.name} ($subRecipeUnitMode)",
                                    color = MacroColors.CarbsAmber,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val q = ingredientQty.toDoubleOrNull() ?: 1.0
                                            if (q > 0.0) {
                                                ingredients.add(
                                                    RecipeIngredient(
                                                        parentRecipeId = parentId,
                                                        subRecipeId = r.recipe.id,
                                                        subRecipeUnitMode = subRecipeUnitMode,
                                                        quantity = q
                                                    )
                                                )
                                            }
                                        }
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (Optional)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ingredients.isNotEmpty()) {
                        val recipe = Recipe(
                            id = parentId,
                            name = name.trim(),
                            totalServings = (servings.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1),
                            totalCookedWeightGrams = cookedWeight.toDoubleOrNull()?.takeIf { it > 0.0 },
                            instructions = instructions.trim()
                        )
                        onSave(recipe, ingredients.toList())
                    }
                }
            ) { Text("Save Recipe") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
