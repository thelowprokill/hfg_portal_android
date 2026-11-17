package com.jon_is_awesome.hfg_portal_app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jon_is_awesome.hfg_portal_app.HfgApplication
import com.jon_is_awesome.hfg_portal_app.ui.budget.AddBudgetScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.AddTransactionScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BucketDetailScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetListScreen
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetRemoteViewModel
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetRemoteViewModelFactory
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.AddCarLogScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogListScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarMaintenanceViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.AddServiceHistoryScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.ServiceHistoryDetailScreen
import com.jon_is_awesome.hfg_portal_app.ui.expenses.AddExpenseScreen
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseListScreen
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseViewModel
import com.jon_is_awesome.hfg_portal_app.ui.expenses.ExpenseViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.loanrepayment.AddRepaymentScreen
import com.jon_is_awesome.hfg_portal_app.ui.loanrepayment.LoanDetailScreen
import com.jon_is_awesome.hfg_portal_app.ui.loanrepayment.LoanListScreen
import com.jon_is_awesome.hfg_portal_app.ui.loanrepayment.LoanRepaymentViewModel
import com.jon_is_awesome.hfg_portal_app.ui.loanrepayment.LoanRepaymentViewModelFactory
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsScreen
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsViewModel
import com.jon_is_awesome.hfg_portal_app.ui.settings.SettingsViewModelFactory

@Composable
fun NavGraph(navController: NavHostController, adminModeEnabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HfgApplication
    val repository = app.repository
    val userPreferencesRepository = app.userPreferencesRepository
    val remoteDataSource = app.remoteDataSource
    
    val carLogViewModel: CarLogViewModel = viewModel(
        factory = CarLogViewModelFactory(app, repository)
    )

    val carMaintenanceViewModel: CarMaintenanceViewModel = viewModel(
        factory = CarMaintenanceViewModelFactory(app, remoteDataSource)
    )
    
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(app, repository)
    )

    val budgetRemoteViewModel: BudgetRemoteViewModel = viewModel(
        factory = BudgetRemoteViewModelFactory(app, remoteDataSource)
    )

    val loanRepaymentViewModel: LoanRepaymentViewModel = viewModel(
        factory = LoanRepaymentViewModelFactory(app, remoteDataSource)
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select a section from the menu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        composable(Screen.CarLogs.route) {
            val userRoles by carMaintenanceViewModel.userRoles.collectAsState()
            val hasAccess = userRoles.isEmpty() || userRoles.containsKey("Car Maintenance")
            LaunchedEffect(userRoles) {
                if (userRoles.isNotEmpty() && !userRoles.containsKey("Car Maintenance")) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            if (hasAccess) {
                val carRole = userRoles["Car Maintenance"]
                CarLogListScreen(
                    viewModel = carLogViewModel,
                    remoteViewModel = carMaintenanceViewModel,
                    canEdit = carRole == "Edit" || carRole == "Admin",
                    onAddLogClick = { navController.navigate("add_service_history") },
                    onServiceHistoryClick = { history ->
                        navController.navigate("service_history_detail/${history.id}")
                    }
                )
            }
        }
        composable("add_service_history") {
            AddServiceHistoryScreen(
                remoteViewModel = carMaintenanceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "service_history_detail/{serviceHistoryId}",
            arguments = listOf(navArgument("serviceHistoryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serviceHistoryId = backStackEntry.arguments?.getInt("serviceHistoryId")
                ?: return@composable
            val userRoles by carMaintenanceViewModel.userRoles.collectAsState()
            val carRole = userRoles["Car Maintenance"]
            ServiceHistoryDetailScreen(
                serviceHistoryId = serviceHistoryId,
                remoteViewModel = carMaintenanceViewModel,
                canEdit = carRole == "Edit" || carRole == "Admin",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("add_car_log") {
            val selectedCar by carMaintenanceViewModel.selectedCar.collectAsState()
            AddCarLogScreen(
                vehicleName = selectedCar?.name,
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
            val userRoles by budgetRemoteViewModel.userRoles.collectAsState()
            val hasAccess = userRoles.isEmpty() || userRoles.containsKey("Budget")
            LaunchedEffect(userRoles) {
                if (userRoles.isNotEmpty() && !userRoles.containsKey("Budget")) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            if (hasAccess) {
                val isBudgetAdmin = userRoles["Budget"] == "Admin" && adminModeEnabled
                BudgetListScreen(
                    viewModel = budgetRemoteViewModel,
                    onBucketClick = { bucket -> navController.navigate("bucket_detail/${bucket.id}") },
                    onOverviewClick = { navController.navigate("account_history") },
                    onBalancesClick = if (isBudgetAdmin) {
                        { navController.navigate("account_balances") }
                    } else null
                )
            }
        }
        composable("account_history") {
            com.jon_is_awesome.hfg_portal_app.ui.budget.AccountHistoryScreen(
                viewModel = budgetRemoteViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("account_balances") {
            val userRoles by budgetRemoteViewModel.userRoles.collectAsState()
            if (userRoles["Budget"] == "Admin" && adminModeEnabled) {
                com.jon_is_awesome.hfg_portal_app.ui.budget.AccountBalancesScreen(
                    viewModel = budgetRemoteViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable("add_bucket") {
            AddBudgetScreen(
                viewModel = budgetRemoteViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "bucket_detail/{bucketId}",
            arguments = listOf(navArgument("bucketId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getInt("bucketId") ?: return@composable
            val userRoles by budgetRemoteViewModel.userRoles.collectAsState()
            val budgetRole = userRoles["Budget"]
            BucketDetailScreen(
                bucketId = bucketId,
                viewModel = budgetRemoteViewModel,
                canEdit = budgetRole == "Edit" || budgetRole == "Admin",
                onNavigateBack = { navController.popBackStack() },
                onAddTransactionClick = { navController.navigate("add_transaction/$bucketId") },
                onEditTransactionClick = { transactionId -> navController.navigate("add_transaction/$bucketId?transactionId=$transactionId") }
            )
        }
        composable(
            "add_transaction/{bucketId}?transactionId={transactionId}",
            arguments = listOf(
                navArgument("bucketId") { type = NavType.IntType },
                navArgument("transactionId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getInt("bucketId") ?: return@composable
            val transactionId = backStackEntry.arguments?.getInt("transactionId")?.takeIf { it != -1 }
            val userRoles by budgetRemoteViewModel.userRoles.collectAsState()
            val budgetRole = userRoles["Budget"]
            AddTransactionScreen(
                bucketId = bucketId,
                viewModel = budgetRemoteViewModel,
                transactionId = transactionId,
                canDelete = budgetRole == "Edit" || budgetRole == "Admin",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LoanRepayment.route) {
            val userRoles by loanRepaymentViewModel.userRoles.collectAsState()
            val hasAccess = userRoles.isEmpty() || userRoles.containsKey("Loan Repayment")
            LaunchedEffect(userRoles) {
                if (userRoles.isNotEmpty() && !userRoles.containsKey("Loan Repayment")) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            if (hasAccess) {
                LoanListScreen(
                    viewModel = loanRepaymentViewModel,
                    onLoanClick = { loan -> navController.navigate("loan_detail/${loan.id}") }
                )
            }
        }
        composable(
            "loan_detail/{loanId}",
            arguments = listOf(navArgument("loanId") { type = NavType.IntType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getInt("loanId") ?: return@composable
            val userRoles by loanRepaymentViewModel.userRoles.collectAsState()
            val loanRole = userRoles["Loan Repayment"]
            LoanDetailScreen(
                loanId = loanId,
                viewModel = loanRepaymentViewModel,
                canEdit = loanRole == "Edit" || loanRole == "Admin",
                canAdmin = loanRole == "Admin" && adminModeEnabled,
                onNavigateBack = { navController.popBackStack() },
                onAddRepaymentClick = { navController.navigate("add_repayment/$loanId") }
            )
        }
        composable(
            "add_repayment/{loanId}",
            arguments = listOf(navArgument("loanId") { type = NavType.IntType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getInt("loanId") ?: return@composable
            AddRepaymentScreen(
                loanId = loanId,
                viewModel = loanRepaymentViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
