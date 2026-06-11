package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

private const val CUSTOM_ITEM_ID = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceHistoryScreen(
    remoteViewModel: CarMaintenanceViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedCar by remoteViewModel.selectedCar.collectAsState()
    val maintenanceItems by remoteViewModel.maintenanceItems.collectAsState()
    val serviceHistory by remoteViewModel.serviceHistory.collectAsState()

    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var namedItem by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }

    LaunchedEffect(serviceHistory) {
        if (mileage.isBlank()) {
            val lastMileage = serviceHistory.mapNotNull { it.mileage }.maxOrNull()
            if (lastMileage != null) mileage = lastMileage.toString()
        }
    }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val selectedLabel = when (selectedItemId) {
        null -> "Select a Maintenance Item"
        CUSTOM_ITEM_ID -> "Other (enter service name)"
        else -> maintenanceItems.find { it.id == selectedItemId }?.name ?: "Unknown"
    }

    val isFormValid = selectedItemId != null &&
        mileage.isNotBlank() &&
        (selectedItemId != CUSTOM_ITEM_ID || namedItem.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Service History") },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            selectedCar?.name?.let { name ->
                Text(text = name, style = MaterialTheme.typography.headlineSmall)
            }

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Maintenance Item") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select a Maintenance Item") },
                        onClick = {},
                        enabled = false
                    )
                    DropdownMenuItem(
                        text = { Text("Other (enter service name)") },
                        onClick = {
                            selectedItemId = CUSTOM_ITEM_ID
                            dropdownExpanded = false
                        }
                    )
                    if (maintenanceItems.isNotEmpty()) {
                        HorizontalDivider()
                        maintenanceItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name ?: "Unknown") },
                                onClick = {
                                    selectedItemId = item.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedItemId == CUSTOM_ITEM_ID) {
                OutlinedTextField(
                    value = namedItem,
                    onValueChange = { namedItem = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text("Mileage") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Service Date: ${formatDate(date)}")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            saveError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val car = selectedCar ?: return@Button
                    val itemId = selectedItemId ?: return@Button
                    val isCustom = itemId == CUSTOM_ITEM_ID
                    val name = if (isCustom) namedItem
                               else maintenanceItems.find { it.id == itemId }?.name ?: ""
                    val maintenanceId = if (isCustom) null else itemId
                    val namedItemValue = if (isCustom) "true" else ""
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(date))

                    isSaving = true
                    saveError = null
                    remoteViewModel.addServiceHistory(
                        carId = car.id,
                        serviceDate = dateStr,
                        mileage = mileage.toIntOrNull() ?: 0,
                        name = name,
                        maintenanceItemId = maintenanceId,
                        namedItem = namedItemValue,
                        notes = notes,
                        onResult = { success ->
                            isSaving = false
                            if (success) onNavigateBack()
                            else saveError = "Failed to save. Please try again."
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
