package com.jon_is_awesome.hfg_portal_app.ui.dashboard

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.jon_is_awesome.hfg_portal_app.ui.budget.BudgetItem
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.CarLogItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToExpenses: () -> Unit,
    onNavigateToCarLogs: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onAddExpense: () -> Unit,
    onAddCarLog: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        viewModel.fetchWeather(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Permission might have been revoked
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HFG Portal Dashboard") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Weather Section
            item {
                WeatherWidget(
                    uiState = uiState,
                    onPermissionRequest = { locationPermissionState.launchMultiplePermissionRequest() }
                )
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Add Expense",
                        icon = Icons.Rounded.AddCircle,
                        modifier = Modifier.weight(1f),
                        onClick = onAddExpense
                    )
                    QuickActionCard(
                        title = "Log Service",
                        icon = Icons.Rounded.DirectionsCar,
                        modifier = Modifier.weight(1f),
                        onClick = onAddCarLog
                    )
                }
            }

            // Expenses Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToExpenses,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Spending",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val formattedSpending = String.format(Locale.getDefault(), "%.2f", uiState.totalExpensesThisMonth)
                        Text(
                            text = "$$formattedSpending",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to view details",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Budget Progress Section
            if (uiState.budgetsWithProgress.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Budgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToBudget) {
                            Text("See All")
                        }
                    }
                }
                items(uiState.budgetsWithProgress.take(2)) { budgetProgress ->
                    BudgetItem(item = budgetProgress, onDeleteClick = {})
                }
            }

            // Recent Car Logs Section
            if (uiState.recentCarLogs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Maintenance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToCarLogs) {
                            Text("See All")
                        }
                    }
                }
                items(uiState.recentCarLogs) { log ->
                    CarLogItem(log = log, onDeleteClick = {})
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WeatherWidget(
    uiState: DashboardUiState,
    onPermissionRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Current Weather",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoadingWeather -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                uiState.weather != null -> {
                    val weather = uiState.weather
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = "${weather.temperature.toInt()}°F",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weather.condition,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                uiState.weatherError != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.weatherError, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Button(onClick = onPermissionRequest) {
                        Text("Get Local Weather")
                    }
                }
            }
        }
    }
}
