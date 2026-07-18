package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteScheduledExpense
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteTransaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketDetailScreen(
    bucketId: Int,
    viewModel: BudgetRemoteViewModel,
    canEdit: Boolean,
    canAdmin: Boolean,
    onNavigateBack: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val buckets by viewModel.buckets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val scheduledExpenses by viewModel.scheduledExpenses.collectAsState()

    val bucket = buckets.find { it.id == bucketId }
    val isSinkingFund = bucket?.isSinkingFund == true

    // Filter scheduled expenses client-side in case the API returns all
    val bucketScheduledExpenses = scheduledExpenses.filter { it.bucket_id == bucketId }

    // Month navigation — shared with BudgetListScreen via ViewModel
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val monthLabel = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance().also {
            it.set(Calendar.MONTH, selectedMonth)
            it.set(Calendar.YEAR, selectedYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Filter transactions to the selected month
    val filteredTransactions = transactions.filter { tx ->
        tx.transaction_date?.let { d ->
            val parts = d.split("-")
            parts.size >= 2
                && parts[0].toIntOrNull() == selectedYear
                && (parts[1].toIntOrNull()?.minus(1)) == selectedMonth
        } ?: false
    }

    // Monthly spend — expenses only (for regular budget buckets)
    val monthlySpent = filteredTransactions
        .filter { it.type == "expense" }
        .mapNotNull { it.amount?.toDoubleOrNull() }.sum()

    // Monthly deposits — the API reports this month's sinking fund contribution directly
    // via monthly_contribution rather than via the transaction list.
    val monthlyDeposits = bucket?.monthly_contribution?.toDoubleOrNull() ?: 0.0

    // Current balance, straight from the server (not re-derived from the transaction list)
    val balanceAtMonthEnd = bucket?.balance?.toDoubleOrNull() ?: 0.0

    var transactionToDelete by remember { mutableStateOf<RemoteTransaction?>(null) }

    LaunchedEffect(bucketId) {
        viewModel.fetchBucketDetail(bucketId)
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Delete \"${transactionToDelete?.description}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val t = transactionToDelete!!
                    transactionToDelete = null
                    viewModel.deleteTransaction(t.id, bucketId) {}
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bucket?.name ?: "Bucket") },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Month navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 0) viewModel.setSelectedMonth(11, selectedYear - 1)
                        else viewModel.setSelectedMonth(selectedMonth - 1, selectedYear)
                    }) {
                        Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        if (selectedMonth == 11) viewModel.setSelectedMonth(0, selectedYear + 1)
                        else viewModel.setSelectedMonth(selectedMonth + 1, selectedYear)
                    }) {
                        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }

            // Summary card
            bucket?.let { b ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!b.description.isNullOrBlank()) {
                                Text(b.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (isSinkingFund) {
                                val goal = b.max_balance?.toDoubleOrNull()
                                val goalReached = goal != null && balanceAtMonthEnd >= goal
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saved", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (b.isEmergencyFund) formatCurrency(balanceAtMonthEnd.toString())
                                        else "${formatCurrency(balanceAtMonthEnd.toString())} / ${formatCurrency(b.max_balance)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (goal != null && goal > 0) {
                                    val progress = (balanceAtMonthEnd / goal).toFloat().coerceIn(0f, 1f)
                                    val color = if (goalReached) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = color,
                                        trackColor = color.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = if (goalReached) "Goal reached!"
                                               else "${formatCurrency((goal - balanceAtMonthEnd).toString())} remaining toward goal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (goalReached) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val contribution = b.monthly_contribution?.toDoubleOrNull()
                                if (contribution != null && contribution > 0) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                                    val overSpent = monthlySpent > contribution
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Spent", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (b.isEmergencyFund) formatCurrency(monthlySpent.toString())
                                            else "${formatCurrency(monthlySpent.toString())} / ${formatCurrency(b.monthly_contribution)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (overSpent) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (!b.isEmergencyFund) {
                                        val spentProgress = (monthlySpent / contribution).toFloat().coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { spentProgress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                            color = if (overSpent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                            trackColor = (if (overSpent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary).copy(alpha = 0.2f)
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                                    val monthFull = monthlyDeposits >= contribution
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Contributed", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${formatCurrency(monthlyDeposits.toString())} / ${formatCurrency(b.monthly_contribution)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (monthFull) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    val monthProgress = (monthlyDeposits / contribution).toFloat().coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { monthProgress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                }
                            } else {
                                val budget = b.monthly_budget?.toDoubleOrNull()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Spent this month", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${formatCurrency(monthlySpent.toString())} / ${formatCurrency(b.monthly_budget)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (budget != null && budget > 0) {
                                    val progress = (monthlySpent / budget).toFloat().coerceIn(0f, 1f)
                                    val overBudget = monthlySpent > budget
                                    val color = if (overBudget) MaterialTheme.colorScheme.error
                                                else if (progress > 0.8f) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.primary
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = color,
                                        trackColor = color.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = if (overBudget)
                                            "Over budget by ${formatCurrency((monthlySpent - budget).toString())}"
                                        else
                                            "${formatCurrency((budget - monthlySpent).toString())} remaining this month",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overBudget) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add New button
            if (canEdit) {
                item {
                    Button(
                        onClick = onAddTransactionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Add New")
                    }
                }
            }

            // Transactions
            if (filteredTransactions.isEmpty()) {
                item {
                    Text(
                        "No transactions for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Description", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                        Text("Amount", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("Type", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("Date", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(40.dp))
                    }
                    HorizontalDivider()
                }
                items(filteredTransactions.sortedByDescending { it.transaction_date }) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tx.description ?: "—", modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(tx.amount), modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        Text(
                            tx.type?.split("_")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) } ?: "—",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(tx.transaction_date ?: "—", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        if (canAdmin) {
                            IconButton(
                                onClick = { transactionToDelete = tx },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Spacer(Modifier.width(40.dp))
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }

            // Scheduled Expenses
            if (bucketScheduledExpenses.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Scheduled Expenses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text("Description", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodySmall)
                        Text("Amount", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        Text("Next Due", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
                items(bucketScheduledExpenses) { se ->
                    ScheduledExpenseRow(se)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ScheduledExpenseRow(se: RemoteScheduledExpense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(se.description ?: "—", modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall)
        Text(formatCurrency(se.amount), modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall)
        Text(se.next_due_date ?: "—", modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider(thickness = 0.5.dp)
}
