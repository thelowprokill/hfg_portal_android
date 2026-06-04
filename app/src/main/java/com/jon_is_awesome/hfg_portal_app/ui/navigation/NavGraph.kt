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
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.AddCarLogScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogListScreen
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModel
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogViewModelFactory

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HfgApplication
    val repository = app.repository
    val carLogViewModel: CarLogViewModel = viewModel(
        factory = CarLogViewModelFactory(app, repository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.CarLogs.route,
        modifier = modifier
    ) {
        composable(Screen.CarLogs.route) {
            CarLogListScreen(
                viewModel = carLogViewModel,
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
            PlaceholderScreen(Screen.Expenses.title)
        }
        composable(Screen.Budget.route) {
            PlaceholderScreen(Screen.Budget.title)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title Screen")
    }
}
