package com.jon_is_awesome.hfg_portal_app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jon_is_awesome.hfg_portal_app.HfgApplication
import com.jon_is_awesome.hfg_portal_app.ui.budget.AddBudgetScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetListScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetViewModel
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.AddCarLogScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogListScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.dashboard.DashboardScreen
import com.jon_is_awesome.hfg_portal_app.ui.dashboard.DashboardViewModel
import com.jon_is_awesome.hfg_portal_app.ui.dashboard.DashboardViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.expenses.AddExpenseScreen
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseListScreen
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseViewModel
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsScreen
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsViewModel
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsViewModelFactory

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HfgApplication
    val repository = app.repository
    val userPreferencesRepository = app.userPreferencesRepository
    val weatherRepository = app.weatherRepository
    val remoteDataSource = app.remoteDataSource
    
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(app, repository, weatherRepository)
    )
    
    val carLogViewModel: CarLogViewModel = viewModel(
        factory = CarLogViewModelFactory(app, repository)
    )

    val carMaintenanceViewModel: CarMaintenanceViewModel = viewModel(
        factory = CarMaintenanceViewModelFactory(app, remoteDataSource)
    )
    
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(app, repository)
    )

    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(app, repository)
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(userPreferencesRepository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToCarLogs = { navController.navigate(Screen.CarLogs.route) },
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                onAddExpense = { navController.navigate("add_expense") },
                onAddCarLog = { navController.navigate("add_car_log") }
            )
        }
        composable(Screen.CarLogs.route) {
            CarLogListScreen(
                viewModel = carLogViewModel,
                remoteViewModel = carMaintenanceViewModel,
                onAddLogClick = { navController.navigate("add_car_log") }
            )
        }
        composable("add_car_log") {
            AddCarLogScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveLog = { log ->
                    carLogViewModel.addCarLog(log)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Expenses.route) {
            ExpenseListScreen(
                viewModel = expenseViewModel,
                onAddExpenseClick = { navController.navigate("add_expense") }
            )
        }
        composable("add_expense") {
            AddExpenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveExpense = { expense ->
                    expenseViewModel.addExpense(expense)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Budget.route) {
            BudgetListScreen(
                viewModel = budgetViewModel,
                onAddBudgetClick = { navController.navigate("add_budget") }
            )
        }
        composable("add_budget") {
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveBudget = { budget ->
                    budgetViewModel.addBudget(budget)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
