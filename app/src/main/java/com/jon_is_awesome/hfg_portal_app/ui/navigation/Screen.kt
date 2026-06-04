package com.jon_is_awesome.hfg_portal_app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object CarLogs : Screen("car_logs", "Car Logs", Icons.Rounded.DirectionsCar)
    object Expenses : Screen("expenses", "Expenses", Icons.Rounded.AccountBalanceWallet)
    object Budget : Screen("budget", "Budget", Icons.Rounded.Analytics)
}

val bottomNavItems = listOf(
    Screen.CarLogs,
    Screen.Expenses,
    Screen.Budget
)
