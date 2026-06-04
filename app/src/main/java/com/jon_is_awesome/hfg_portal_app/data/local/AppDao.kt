package com.jon_is_awesome.hfg_portal_app.data.local

import androidx.room.*
import com.jon_is_awesome.hfg_portal_app.data.model.Budget
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import com.jon_is_awesome.hfg_portal_app.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Car Logs
    @Query("SELECT * FROM car_logs ORDER BY date DESC")
    fun getAllCarLogs(): Flow<List<CarLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarLog(carLog: CarLog)

    @Delete
    suspend fun deleteCarLog(carLog: CarLog)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Budgets
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}
