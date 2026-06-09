package com.jon_is_awesome.hfg_portal_app.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.model.Budget
import com.jon_is_awesome.hfg_portal_app.data.model.Expense
import com.jon_is_awesome.hfg_portal_app.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetWithProgress(
    val budget: Budget,
    val spentAmount: Double
)

class BudgetViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    val budgetsWithProgress: StateFlow<List<BudgetWithProgress>> = repository.getAllBudgets()
        .combine(repository.getAllExpenses()) { budgets, expenses ->
            budgets.map { budget ->
                val spent = expenses
                    .filter { it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
                BudgetWithProgress(budget, spent)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }
}

class BudgetViewModelFactory(private val application: Application, private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
