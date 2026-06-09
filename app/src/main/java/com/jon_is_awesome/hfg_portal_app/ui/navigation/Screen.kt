package com.jon_is_awesome.hfg_portal_app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    object CarLogs : Screen("car_logs", "Car Logs", Icons.Rounded.DirectionsCar)
    object Expenses : Screen("expenses", "Expenses", Icons.Rounded.AccountBalanceWallet)
    object Budget : Screen("budget", "Budget", Icons.Rounded.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.CarLogs,
    Screen.Expenses,
    Screen.Budget,
    Screen.Settings
)
