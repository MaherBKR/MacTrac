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
                                        text = "${recipeItem.ingredients.size} ingredients • ${recipeItem.recipe.totalServings} servings
