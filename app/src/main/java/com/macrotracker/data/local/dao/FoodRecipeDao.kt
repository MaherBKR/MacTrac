package com.macrotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.macrotracker.data.local.entity.FoodItem
import com.macrotracker.data.local.entity.FoodUnit
import com.macrotracker.data.local.entity.Recipe
import com.macrotracker.data.local.entity.RecipeIngredient
import com.macrotracker.data.local.relation.FoodWithUnits
import com.macrotracker.data.local.relation.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodRecipeDao {

    // --- Food Items & Units ---
    @Transaction
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAllFoodsWithUnits(): Flow<List<FoodWithUnits>>

    @Transaction
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFoodsWithUnits(query: String): Flow<List<FoodWithUnits>>

    @Transaction
    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodWithUnitsById(id: Long): FoodWithUnits?

    @Transaction
    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodWithUnits?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: FoodUnit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<FoodUnit>)

    @Update
    suspend fun updateUnit(unit: FoodUnit)

    @Delete
    suspend fun deleteUnit(unit: FoodUnit)

    @Query("DELETE FROM food_units WHERE foodItemId = :foodId")
    suspend fun deleteUnitsForFood(foodId: Long)

    // --- Recipes & Nested Ingredients ---
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun observeAllRecipesWithIngredients(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeWithIngredients(recipeId: Long): RecipeWithIngredients?

    @Query("SELECT * FROM recipe_ingredients WHERE parentRecipeId = :recipeId")
    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE parentRecipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)

    @Transaction
    suspend fun saveRecipeWithIngredients(recipe: Recipe, ingredients: List<RecipeIngredient>): Long {
        val recipeId = if (recipe.id == 0L) {
            insertRecipe(recipe)
        } else {
            updateRecipe(recipe)
            deleteIngredientsForRecipe(recipe.id)
            recipe.id
        }
        val boundIngredients = ingredients.map { it.copy(id = 0L, parentRecipeId = recipeId) }
        insertRecipeIngredients(boundIngredients)
        return recipeId
    }
}
