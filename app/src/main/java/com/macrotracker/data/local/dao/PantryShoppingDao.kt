package com.macrotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.macrotracker.data.local.entity.PantryItem
import com.macrotracker.data.local.entity.ShoppingExpense
import com.macrotracker.data.local.entity.ShoppingExpenseItem
import com.macrotracker.data.local.entity.ShoppingItem
import com.macrotracker.data.local.relation.ExpenseWithItems
import com.macrotracker.data.local.relation.PeriodSpendSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryShoppingDao {

    // --- Pantry ---
    @Query("SELECT * FROM pantry_items ORDER BY category ASC, name ASC")
    fun observeAllPantryItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem): Long

    @Update
    suspend fun updatePantryItem(item: PantryItem)

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    // --- Shopping List ---
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, category ASC, name ASC")
    fun observeAllShoppingItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteCheckedShoppingItems()

    // --- Shopping Expenses & Analytics ---
    @Transaction
    @Query("SELECT * FROM shopping_expenses ORDER BY timestamp DESC")
    fun observeAllExpensesWithItems(): Flow<List<ExpenseWithItems>>

    @Query(
        """
        SELECT weekKey AS periodKey, SUM(totalAmount) AS totalSpent, COUNT(*) AS tripCount
        FROM shopping_expenses
        GROUP BY weekKey
        ORDER BY weekKey DESC
        LIMIT 12
        """
    )
    fun observeWeeklySpendSummaries(): Flow<List<PeriodSpendSummary>>

    @Query(
        """
        SELECT monthKey AS periodKey, SUM(totalAmount) AS totalSpent, COUNT(*) AS tripCount
        FROM shopping_expenses
        GROUP BY monthKey
        ORDER BY monthKey DESC
        LIMIT 12
        """
    )
    fun observeMonthlySpendSummaries(): Flow<List<PeriodSpendSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ShoppingExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseItems(items: List<ShoppingExpenseItem>)

    @Delete
    suspend fun deleteExpense(expense: ShoppingExpense)

    @Transaction
    suspend fun recordExpenseWithItems(
        expense: ShoppingExpense,
        items: List<ShoppingExpenseItem>
    ): Long {
        val expenseId = insertExpense(expense)
        insertExpenseItems(items.map { it.copy(id = 0L, expenseId = expenseId) })
        return expenseId
    }
}
