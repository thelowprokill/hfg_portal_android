package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    viewModel: BudgetViewModel,
    onAddBudgetClick: () -> Unit
) {
    val budgetsWithProgress by viewModel.budgetsWithProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBudgetClick) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        if (budgetsWithProgress.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No budgets set. Start by adding one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgetsWithProgress) { item ->
                    BudgetItem(
                        item = item,
                        onDeleteClick = { viewModel.deleteBudget(it.budget) }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetItem(
    item: BudgetWithProgress,
    onDeleteClick: (BudgetWithProgress) -> Unit
) {
    val progress = if (item.budget.limitAmount > 0) (item.spentAmount / item.budget.limitAmount).toFloat() else 0f
    val isOverBudget = item.spentAmount > item.budget.limitAmount
    val progressColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.budget.category,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onDeleteClick(item) }) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Budget",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "Spent: $%.2f", item.spentAmount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = String.format(Locale.getDefault(), "Limit: $%.2f", item.budget.limitAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )

            if (isOverBudget) {
                Text(
                    text = String.format(Locale.getDefault(), "Over budget by $%.2f", item.spentAmount - item.budget.limitAmount),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = String.format(Locale.getDefault(), "Remaining: $%.2f", item.budget.limitAmount - item.spentAmount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
