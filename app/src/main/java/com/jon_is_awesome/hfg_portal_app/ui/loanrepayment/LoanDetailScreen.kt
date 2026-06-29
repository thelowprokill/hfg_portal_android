package com.jon_is_awesome.hfg_portal_app.ui.loanrepayment

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
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteRepayment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Int,
    viewModel: LoanRepaymentViewModel,
    canEdit: Boolean,
    canAdmin: Boolean,
    onNavigateBack: () -> Unit,
    onAddRepaymentClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsState()
    val repayments by viewModel.repayments.collectAsState()
    val loan = loans.find { it.id == loanId }
    val isOverpaid = loan?.remaining_balance?.toDoubleOrNull()?.let { it < 0 } == true

    LaunchedEffect(loanId) { viewModel.fetchRepayments(loanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loans.find { it.id == loanId }?.loanFor ?: "Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = onAddRepaymentClick) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Payment")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                loan?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = it.loanFor ?: "Loan #${it.id}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!it.paid_to.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = it.paid_to,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!it.description.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = it.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DetailStat(
                                    label = "Original",
                                    value = "$${it.amount ?: "—"}"
                                )
                                DetailStat(
                                    label = "Paid Total",
                                    value = "$${it.paid_amount_tot ?: "0.00"}"
                                )
                                DetailStat(
                                    label = "Remaining",
                                    value = "$${it.remaining_balance ?: "0.00"}",
                                    highlight = isOverpaid
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (repayments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No payments recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(repayments, key = { it.id }) { repayment ->
                    RepaymentItem(
                        repayment = repayment,
                        canAdmin = canAdmin,
                        onDelete = { viewModel.deleteRepayment(repayment.id, loanId) {} }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, highlight: Boolean = false) {
    val valueColor = if (highlight) MaterialTheme.colorScheme.tertiary
                     else MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun RepaymentItem(
    repayment: RemoteRepayment,
    canAdmin: Boolean,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete Payment") },
            text = { Text("Remove this payment? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = repayment.inserted_at?.take(10) ?: "Payment #${repayment.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${repayment.paid_amount ?: "0.00"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!repayment.paid_amount_tot.isNullOrBlank()) {
                        Text(
                            text = "Total paid: $${repayment.paid_amount_tot}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!repayment.remaining_balance.isNullOrBlank()) {
                        Text(
                            text = "Balance: $${repayment.remaining_balance}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (canAdmin) {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
