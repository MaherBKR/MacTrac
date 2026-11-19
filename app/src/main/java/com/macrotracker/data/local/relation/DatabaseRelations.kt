package com.macrotracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.Recipe
import com.macrotracker.data.local.entity.RecipeIngredient
import com.macrotracker.data.local.entity.ShoppingExpense
import com.macrotracker.data.local.entity.ShoppingExpenseItem

data class FoodWithUnits(
    @Embedded val food: FoodItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "foodItemId"
    )
    val units: List<FoodUnit>
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentRecipeId"
    )
    val ingredients: List<RecipeIngredient>
)

data class ExpenseWithItems(
    @Embedded val expense: ShoppingExpense,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val items: List<ShoppingExpenseItem>
)

data class PeriodSpendSummary(
    val periodKey: String,
    val totalSpent: Double,
    val tripCount: Int
)

data class DailyMacroSummary(
    val date: String,
    val totalKcal: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)

data class DailyWaterSummary(
    val date: String,
    val totalMl: Int
)
