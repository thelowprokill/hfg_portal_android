package com.jon_is_awesome.hfg_portal_app.ui.budget

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
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.formatDate
import java.text.SimpleDateFormat
import java.util.*

private val TRANSACTION_TYPES = listOf("expense", "contribution", "sinking_fund_deposit")

private fun formatTransactionType(type: String): String =
    type.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    bucketId: Int,
    viewModel: BudgetRemoteViewModel,
    onNavigateBack: () -> Unit
) {
    val buckets by viewModel.buckets.collectAsState()
    val bucket = buckets.find { it.id == bucketId }
    val bucketName = bucket?.name
    val isSinkingFund = bucket?.monthly_budget.toDoubleOrNullSafe() == null

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var type by remember(isSinkingFund) { mutableStateOf(if (isSinkingFund) "sinking_fund_deposit" else "expense") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
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
            if (!bucketName.isNullOrBlank()) {
                Text(bucketName, style = MaterialTheme.typography.headlineSmall)
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = formatTransactionType(type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    TRANSACTION_TYPES.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                type = t
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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
                Text("Date: ${formatDate(date)}")
            }

            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(date))
                    isSaving = true
                    saveError = null
                    viewModel.addTransaction(
                        bucketId = bucketId,
                        amount = amount.trim(),
                        description = description.trim(),
                        date = dateStr,
                        type = type,
                        onResult = { success ->
                            isSaving = false
                            if (success) onNavigateBack()
                            else saveError = "Failed to save. Please try again."
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank() && description.isNotBlank() && !isSaving
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
