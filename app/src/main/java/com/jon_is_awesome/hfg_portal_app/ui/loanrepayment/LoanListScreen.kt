package com.jon_is_awesome.hfg_portal_app.ui.loanrepayment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteLoan

@Composable
fun LoanListScreen(
    viewModel: LoanRepaymentViewModel,
    onLoanClick: (RemoteLoan) -> Unit
) {
    val loans by viewModel.loans.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchLoans() }

    if (loans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No loans found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(loans, key = { it.id }) { loan ->
                LoanCard(loan = loan, onClick = { onLoanClick(loan) })
            }
        }
    }
}

@Composable
private fun LoanCard(loan: RemoteLoan, onClick: () -> Unit) {
    val isOverpaid = loan.remaining_balance?.toDoubleOrNull()?.let { it < 0 } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.loanFor ?: "Loan #${loan.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!loan.paid_to.isNullOrBlank()) {
                        Text(
                            text = loan.paid_to,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${loan.remaining_balance ?: "0.00"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverpaid) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LoanStat(label = "Original", value = "$${loan.amount ?: "—"}")
                LoanStat(label = "Paid Total", value = "$${loan.paid_amount_tot ?: "0.00"}")
                if (loan.active != null) {
                    LoanStat(label = "Status", value = if (loan.active) "Active" else "Closed")
                }
            }
        }
    }
}

@Composable
private fun LoanStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
