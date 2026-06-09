package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteScheduledExpense
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketDetailScreen(
    bucketId: Int,
    viewModel: BudgetRemoteViewModel,
    onNavigateBack: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val buckets by viewModel.buckets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val scheduledExpenses by viewModel.scheduledExpenses.collectAsState()

    val bucket = buckets.find { it.id == bucketId }

    var transactionToDelete by remember { mutableStateOf<RemoteTransaction?>(null) }
    var showDeleteBucketDialog by remember { mutableStateOf(false) }

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

    if (showDeleteBucketDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBucketDialog = false },
            title = { Text("Delete Bucket") },
            text = { Text("Delete \"${bucket?.name}\"? This will also delete all its transactions and scheduled expenses.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteBucketDialog = false
                    viewModel.deleteBucket(bucketId) { onNavigateBack() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBucketDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bucket?.name ?: "Bucket") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteBucketDialog = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Bucket",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransactionClick) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Bucket info card
            bucket?.let { b ->
                val isSinkingFund = b.monthly_budget.toDoubleOrNullSafe() == null
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!b.description.isNullOrBlank()) {
                                Text(b.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (isSinkingFund) {
                                // Sinking fund: show balance vs goal
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LabeledValue("Saved", formatCurrency(b.balance))
                                    LabeledValue("Goal", formatCurrency(b.max_balance))
                                    LabeledValue("Monthly +", formatCurrency(b.monthly_contribution))
                                }
                                val saved = b.balance?.toDoubleOrNull() ?: 0.0
                                val goal = b.max_balance?.toDoubleOrNull()
                                if (goal != null && goal > 0) {
                                    val progress = (saved / goal).toFloat().coerceIn(0f, 1f)
                                    val goalReached = saved >= goal
                                    val color = if (goalReached) MaterialTheme.colorScheme.primary
                                                else if (progress > 0.8f) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.secondary
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = color,
                                        trackColor = color.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = if (goalReached) "Goal reached!"
                                               else "${formatCurrency((goal - saved).toString())} remaining toward goal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (goalReached) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // Budget bucket: show balance vs monthly budget
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LabeledValue("Balance", formatCurrency(b.balance))
                                    LabeledValue("Monthly Budget", formatCurrency(b.monthly_budget))
                                }
                                val balance = b.balance?.toDoubleOrNull()
                                val budget = b.monthly_budget?.toDoubleOrNull()
                                if (balance != null && budget != null && budget > 0) {
                                    val progress = (balance / budget).toFloat().coerceIn(0f, 1f)
                                    val overBudget = balance > budget
                                    val color = if (overBudget) MaterialTheme.colorScheme.error
                                                else if (progress > 0.8f) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.primary
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = color,
                                        trackColor = color.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = if (overBudget)
                                            "Over budget by ${formatCurrency((balance - budget).toString())}"
                                        else
                                            "${formatCurrency((budget - balance).toString())} remaining",
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

            // Transactions section
            item {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.bodySmall)
                        Text("Description", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodySmall)
                        Text("Amount", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(40.dp))
                    }
                    HorizontalDivider()
                }
                items(transactions.sortedByDescending { it.transaction_date }) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tx.transaction_date ?: "—", modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.bodySmall)
                        Text(tx.description ?: "—", modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodySmall)
                        Text(
                            formatCurrency(tx.amount),
                            modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (tx.type == "credit") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { transactionToDelete = tx },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }

            // Scheduled Expenses section
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Scheduled Expenses",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (scheduledExpenses.isEmpty()) {
                item {
                    Text(
                        "No scheduled expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
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
                items(scheduledExpenses) { se ->
                    ScheduledExpenseRow(se)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScheduledExpenseRow(se: RemoteScheduledExpense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
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
