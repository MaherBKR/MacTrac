package com.macrotracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["barcode"])]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultBaseUnit: String = "g", // "g" or "ml"
    val isBuiltIn: Boolean = false
)

@Serializable
@Entity(
    tableName = "food_units",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["foodItemId"])]
)
data class FoodUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val foodItemId: Long,
    val unitName: String,       // e.g., "Gram", "Large Egg", "Scoop"
    val gramEquivalent: Double  // e.g., 1.0, 50.0, 30.0
)

@Serializable
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val totalServings: Double = 1.0,
    val totalCookedWeightGrams: Double? = null,
    val instructions: String = ""
)

@Serializable
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["parentRecipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["subRecipeId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FoodUnit::class,
            parentColumns = ["id"],
            childColumns = ["foodUnitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["parentRecipeId"]),
        Index(value = ["foodItemId"]),
        Index(value = ["subRecipeId"]),
        Index(value = ["foodUnitId"])
    ]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val parentRecipeId: Long,
    val foodItemId: Long? = null,
    val subRecipeId: Long? = null,
    val foodUnitId: Long? = null,
    val subRecipeUnitMode: String? = null, // "SERVING" or "GRAMS" when subRecipeId != null
    val quantity: Double
)
