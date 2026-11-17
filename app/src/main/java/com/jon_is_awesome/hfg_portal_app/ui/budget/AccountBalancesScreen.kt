package com.jon_is_awesome.hfg_portal_app.ui.budget

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteAccountBalance
import com.jon_is_awesome.hfg_portal_app.ui.carlogs.formatDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBalancesScreen(
    viewModel: BudgetRemoteViewModel,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accountBalances.collectAsState()
    var editing by remember { mutableStateOf<RemoteAccountBalance?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchAccountBalances() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Balances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.account_id }) { account ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.name ?: "—", style = MaterialTheme.typography.titleMedium)
                            Text(
                                account.type?.replaceFirstChar(Char::uppercase) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                account.balance?.toDoubleOrNull()?.let { "$%,.2f".format(it) } ?: "No balance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "As of ${account.date ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { editing = account }) { Text("Update") }
                    }
                }
            }
        }
    }

    editing?.let { account ->
        UpdateBalanceDialog(
            account = account,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBalanceDialog(
    account: RemoteAccountBalance,
    viewModel: BudgetRemoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableLongStateOf(todayAsUtcMillis()) }
    var value by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ${account.name ?: "balance"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: ${formatDate(date)}")
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && value.toBigDecimalOrNull() != null,
                onClick = {
                    saving = true
                    // date millis are UTC midnight of the chosen local day (see todayAsUtcMillis)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .format(Date(date))
                    viewModel.updateAccountBalance(account.account_id, value, dateStr) { ok ->
                        saving = false
                        if (ok) onDismiss()
                        else Toast.makeText(context, "Failed to update balance", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
