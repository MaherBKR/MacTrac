package com.macrotracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "pantry_items",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["foodItemId"])]
)
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val foodItemId: Long? = null,
    val quantity: Double,
    val unitLabel: String = "pcs",      // "pcs", "kg", "tubs", "bottles"
    val category: String = "Dry Pantry", // "Fridge", "Freezer", "Dry Pantry"
    val expiryDate: String? = null,      // Optional YYYY-MM-DD
    val minThreshold: Double = 1.0
)

@Serializable
@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = PantryItem::class,
            parentColumns = ["id"],
            childColumns = ["pantryItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["pantryItemId"])]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val quantity: Double = 1.0,
    val unitLabel: String = "pcs",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "Groceries",
    val isChecked: Boolean = false,
    val pantryItemId: Long? = null
)

@Serializable
@Entity(
    tableName = "shopping_expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["weekKey"]),
        Index(value = ["monthKey"])
    ]
)
data class ShoppingExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,           // YYYY-MM-DD
    val weekKey: String,        // YYYY-Www (e.g., 2026-W47)
    val monthKey: String,       // YYYY-MM (e.g., 2026-11)
    val totalAmount: Double,
    val itemCount: Int,
    val storeOrNote: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "shopping_expense_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingExpense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["expenseId"])]
)
data class ShoppingExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expenseId: Long,
    val itemName: String,
    val category: String,
    val quantity: Double,
    val unitLabel: String,
    val pricePaid: Double
)
