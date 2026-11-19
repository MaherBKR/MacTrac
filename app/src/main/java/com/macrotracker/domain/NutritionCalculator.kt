package com.macrotracker.domain

import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.local.relation.RecipeWithIngredients

data class MacroBreakdown(
    val grams: Double = 0.0,
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
) {
    operator fun plus(other: MacroBreakdown) = MacroBreakdown(
        grams = grams + other.grams,
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat
    )

    fun scale(factor: Double) = MacroBreakdown(
        grams = grams * factor,
        kcal = kcal * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor
    )
}

object NutritionCalculator {

    fun calculateFoodMacros(
        food: FoodItem,
        unit: FoodUnit?,
        quantity: Double
    ): MacroBreakdown {
        val gramEquivalent = unit?.gramEquivalent ?: 1.0
        val totalGrams = (quantity * gramEquivalent).coerceAtLeast(0.0)
        val factor = totalGrams / 100.0
        return MacroBreakdown(
            grams = totalGrams,
            kcal = food.kcalPer100g * factor,
            protein = food.proteinPer100g * factor,
            carbs = food.carbsPer100g * factor,
            fat = food.fatPer100g * factor
        )
    }

    /**
     * Checks if adding [candidateSubRecipeId] into [parentRecipeId] would create a cycle.
     * Returns true if a cycle is detected (meaning the addition MUST be blocked).
     */
    fun wouldCreateCycle(
        parentRecipeId: Long,
        candidateSubRecipeId: Long,
        allRecipes: Map<Long, RecipeWithIngredients>
    ): Boolean {
        if (parentRecipeId == 0L) return false
        if (parentRecipeId == candidateSubRecipeId) return true
        val visited = mutableSetOf<Long>()
        return hasPathToTarget(candidateSubRecipeId, parentRecipeId, allRecipes, visited)
    }

    private fun hasPathToTarget(
        currentId: Long,
        targetId: Long,
        allRecipes: Map<Long, RecipeWithIngredients>,
        visited: MutableSet<Long>
    ): Boolean {
        if (currentId == targetId) return true
        if (!visited.add(currentId)) return false
        val recipe = allRecipes[currentId] ?: return false
        for (ingredient in recipe.ingredients) {
            val subId = ingredient.subRecipeId ?: continue
            if (hasPathToTarget(subId, targetId, allRecipes, visited)) return true
        }
        return false
    }

    /**
     * Recursively computes total macros for an entire recipe batch.
     * Uses [visited] to guard against any corrupted circular references.
     */
    fun calculateRecipeTotalMacros(
        recipeId: Long,
        allFoods: Map<Long, FoodWithUnits>,
        allRecipes: Map<Long, RecipeWithIngredients>,
        visited: MutableSet<Long> = mutableSetOf()
    ): MacroBreakdown {
        if (!visited.add(recipeId)) return MacroBreakdown()
        val recipeWithIngredients = allRecipes[recipeId] ?: return MacroBreakdown()

        var total = MacroBreakdown()
        for (ingredient in recipeWithIngredients.ingredients) {
            if (ingredient.foodItemId != null) {
                val foodWithUnits = allFoods[ingredient.foodItemId] ?: continue
                val unit = foodWithUnits.units.find { it.id == ingredient.foodUnitId }
                total += calculateFoodMacros(foodWithUnits.food, unit, ingredient.quantity)
            } else if (ingredient.subRecipeId != null) {
                val subMacros = calculateRecipePortionMacros(
                    recipeId = ingredient.subRecipeId,
                    unitMode = ingredient.subRecipeUnitMode ?: "SERVING",
                    quantity = ingredient.quantity,
                    allFoods = allFoods,
                    allRecipes = allRecipes,
                    visited = visited.toMutableSet()
                )
                total += subMacros
            }
        }

        val finalGrams = recipeWithIngredients.recipe.totalCookedWeightGrams ?: total.grams
        return total.copy(grams = finalGrams)
    }

    /**
     * Computes macros for a logged portion of a recipe (either by SERVING or by GRAMS).
     */
    fun calculateRecipePortionMacros(
        recipeId: Long,
        unitMode: String, // "SERVING" or "GRAMS"
        quantity: Double,
        allFoods: Map<Long, FoodWithUnits>,
        allRecipes: Map<Long, RecipeWithIngredients>,
        visited: MutableSet<Long> = mutableSetOf()
    ): MacroBreakdown {
        val recipe = allRecipes[recipeId]?.recipe ?: return MacroBreakdown()
        val batchTotal = calculateRecipeTotalMacros(recipeId, allFoods, allRecipes, visited)
        if (quantity <= 0.0) return MacroBreakdown()

        return if (unitMode.equals("GRAMS", ignoreCase = true)) {
            val batchGrams = recipe.totalCookedWeightGrams ?: batchTotal.grams
            if (batchGrams <= 0.0) MacroBreakdown()
            else batchTotal.scale(quantity / batchGrams).copy(grams = quantity)
        } else {
            val servings = recipe.totalServings.coerceAtLeast(0.01)
            batchTotal.scale(quantity / servings)
        }
    }
}
