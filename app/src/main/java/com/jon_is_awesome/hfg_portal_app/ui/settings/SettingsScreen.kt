package com.jon_is_awesome.hfg_portal_app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val userName by viewModel.userName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()

    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userName) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") }, windowInsets = WindowInsets(0))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("User Name", style = MaterialTheme.typography.labelMedium)
                            if (editingName) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                )
                            } else {
                                Text(userName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        TextButton(onClick = {
                            if (editingName) {
                                viewModel.updateUserName(nameInput)
                            } else {
                                nameInput = userName
                            }
                            editingName = !editingName
                        }) {
                            Text(if (editingName) "Save" else "Edit")
                        }
                    }
                }
            }

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Currency Symbol", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("$", "€", "£", "¥").forEach { symbol ->
                                FilterChip(
                                    selected = currencySymbol == symbol,
                                    onClick = { viewModel.updateCurrencySymbol(symbol) },
                                    label = { Text(symbol) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Column {
                        Text("Temperature Unit", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = temperatureUnit == "fahrenheit",
                                onClick = { viewModel.updateTemperatureUnit("fahrenheit") },
                                label = { Text("Fahrenheit (°F)") }
                            )
                            FilterChip(
                                selected = temperatureUnit == "celsius",
                                onClick = { viewModel.updateTemperatureUnit("celsius") },
                                label = { Text("Celsius (°C)") }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "HFG Portal v1.0.0",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
