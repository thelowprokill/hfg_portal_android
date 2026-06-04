package com.jon_is_awesome.hfg_portal_app.data.repository

import com.jon_is_awesome.hfg_portal_app.data.local.AppDao
import com.jon_is_awesome.hfg_portal_app.data.model.Budget
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import com.jon_is_awesome.hfg_portal_app.data.model.Expense
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    // Car Logs
    fun getAllCarLogs(): Flow<List<CarLog>> = appDao.getAllCarLogs()
    suspend fun insertCarLog(carLog: CarLog) = appDao.insertCarLog(carLog)
    suspend fun deleteCarLog(carLog: CarLog) = appDao.deleteCarLog(carLog)

    // Expenses
    fun getAllExpenses(): Flow<List<Expense>> = appDao.getAllExpenses()
    suspend fun insertExpense(expense: Expense) = appDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = appDao.deleteExpense(expense)

    // Budgets
    fun getAllBudgets(): Flow<List<Budget>> = appDao.getAllBudgets()
    suspend fun insertBudget(budget: Budget) = appDao.insertBudget(budget)
    suspend fun deleteBudget(budget: Budget) = appDao.deleteBudget(budget)
}
