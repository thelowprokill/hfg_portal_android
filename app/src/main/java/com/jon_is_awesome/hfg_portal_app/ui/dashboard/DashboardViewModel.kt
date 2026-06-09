package com.jon_is_awesome.hfg_portal_app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.model.Budget
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import com.jon_is_awesome.hfg_portal_app.data.model.Expense
import com.jon_is_awesome.hfg_portal_app.data.model.WeatherInfo
import com.jon_is_awesome.hfg_portal_app.data.repository.AppRepository
import com.jon_is_awesome.hfg_portal_app.data.repository.WeatherRepository
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetWithProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardUiState(
    val totalExpensesThisMonth: Double = 0.0,
    val recentCarLogs: List<CarLog> = emptyList(),
    val budgetsWithProgress: List<BudgetWithProgress> = emptyList(),
    val weather: WeatherInfo? = null,
    val isLoadingWeather: Boolean = false,
    val weatherError: String? = null
)

class DashboardViewModel(
    application: Application,
    private val repository: AppRepository,
    private val weatherRepository: WeatherRepository
) : AndroidViewModel(application) {

    private val _weatherState = MutableStateFlow<WeatherInfo?>(null)
    private val _isLoadingWeather = MutableStateFlow(false)
    private val _weatherError = MutableStateFlow<String?>(null)

    private val weatherCombined = combine(_weatherState, _isLoadingWeather, _weatherError) { weather, isLoading, error ->
        Triple(weather, isLoading, error)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllExpenses(),
        repository.getAllCarLogs(),
        repository.getAllBudgets(),
        weatherCombined
    ) { expenses: List<Expense>, carLogs: List<CarLog>, budgets: List<Budget>, weatherTriple: Triple<WeatherInfo?, Boolean, String?> ->
        val (weather, isLoading, error) = weatherTriple
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val totalExpenses = expenses.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.date }
            expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        val recentLogs = carLogs.take(3)

        val budgetsProgress = budgets.map { budget ->
            val spent = expenses
                .filter { expense -> expense.category.lowercase() == budget.category.lowercase() }
                .sumOf { it.amount }
            BudgetWithProgress(budget, spent)
        }

        DashboardUiState(
            totalExpensesThisMonth = totalExpenses,
            recentCarLogs = recentLogs,
            budgetsWithProgress = budgetsProgress,
            weather = weather,
            isLoadingWeather = isLoading,
            weatherError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoadingWeather.value = true
            _weatherError.value = null
            try {
                val weather = weatherRepository.getWeatherData(lat, lon)
                _weatherState.value = weather
            } catch (e: Exception) {
                _weatherError.value = "Failed to load weather: ${e.message}"
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }
}

class DashboardViewModelFactory(
    private val application: Application,
    private val repository: AppRepository,
    private val weatherRepository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository, weatherRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
