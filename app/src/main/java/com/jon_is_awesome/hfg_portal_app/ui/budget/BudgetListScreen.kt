package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteBucket
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteTransaction
import java.text.SimpleDateFormat
import java.util.*

private val SORT_FIELDS = listOf(
    "name" to "Name",
    "balance" to "Balance",
    "monthly_budget" to "Budget",
    "monthly_contribution" to "Contribution"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    viewModel: BudgetRemoteViewModel,
    onBucketClick: (RemoteBucket) -> Unit
) {
    val buckets by viewModel.buckets.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val accountTotals by viewModel.accountTotals.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var sortDropdownExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    // Scroll past the search/sort items on first load so they're hidden above the fold
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            listState.scrollToItem(2)
        }
    }

    val monthLabel = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance().also {
            it.set(Calendar.MONTH, selectedMonth)
            it.set(Calendar.YEAR, selectedYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val thisMonthTransactions = remember(allTransactions, selectedMonth, selectedYear) {
        allTransactions.filter { tx ->
            tx.transaction_date?.let { d ->
                val parts = d.split("-")
                parts.size >= 2
                    && parts[0].toIntOrNull() == selectedYear
                    && (parts[1].toIntOrNull()?.minus(1)) == selectedMonth
            } ?: false
        }
    }

    val monthlySpentByBucket = remember(thisMonthTransactions) {
        thisMonthTransactions
            .filter { it.type == "expense" }
            .groupBy { it.bucket_id ?: -1 }
            .mapValues { (_, txs) -> txs.mapNotNull { it.amount?.toDoubleOrNull() }.sum() }
    }

    val monthlyDepositsByBucket = remember(thisMonthTransactions) {
        thisMonthTransactions
            .filter { it.type == "sinking_fund_deposit" || it.type == "contribution" }
            .groupBy { it.bucket_id ?: -1 }
            .mapValues { (_, txs) -> txs.mapNotNull { it.amount?.toDoubleOrNull() }.sum() }
    }

    val balanceAtMonthEndByBucket = remember(allTransactions, selectedMonth, selectedYear) {
        allTransactions
            .filter { tx ->
                tx.transaction_date?.let { d ->
                    val parts = d.split("-")
                    if (parts.size < 2) return@filter false
                    val txYear = parts[0].toIntOrNull() ?: return@filter false
                    val txMonth = (parts[1].toIntOrNull() ?: return@filter false) - 1
                    txYear < selectedYear || (txYear == selectedYear && txMonth <= selectedMonth)
                } ?: false
            }
            .groupBy { it.bucket_id ?: -1 }
            .mapValues { (_, txs) ->
                txs.mapNotNull { tx ->
                    val amount = tx.amount?.toDoubleOrNull() ?: return@mapNotNull null
                    if (tx.type == "expense") -amount else amount
                }.sum()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                windowInsets = WindowInsets(0),
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (isLoggedIn) {
                true -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearch(it) },
                                placeholder = { Text("Search buckets…") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = sortDropdownExpanded,
                                    onExpandedChange = { sortDropdownExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = SORT_FIELDS.find { it.first == sortField }?.second ?: sortField,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Sort by") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortDropdownExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sortDropdownExpanded,
                                        onDismissRequest = { sortDropdownExpanded = false }
                                    ) {
                                        SORT_FIELDS.forEach { (value, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    viewModel.setSort(value, sortOrder)
                                                    sortDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    viewModel.setSort(sortField, if (sortOrder == "asc") "desc" else "asc")
                                }) {
                                    Icon(
                                        if (sortOrder == "asc") Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = if (sortOrder == "asc") "Ascending" else "Descending"
                                    )
                                }
                            }
                        }
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
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                                }
                                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    if (selectedMonth == 11) viewModel.setSelectedMonth(0, selectedYear + 1)
                                    else viewModel.setSelectedMonth(selectedMonth + 1, selectedYear)
                                }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                                }
                            }
                        }

                        item {
                            val regularBuckets = buckets.filter { it.monthly_budget.toDoubleOrNullSafe() != null }
                            val totalBudget = regularBuckets.mapNotNull { it.monthly_budget?.toDoubleOrNull() }.sum()
                            val totalSpent = regularBuckets.sumOf { monthlySpentByBucket[it.id] ?: 0.0 }
                            val sinkingBuckets = buckets.filter { it.monthly_budget.toDoubleOrNullSafe() == null }
                            val totalSaved = sinkingBuckets.sumOf { balanceAtMonthEndByBucket[it.id] ?: 0.0 }
                            val totalGoal = sinkingBuckets.mapNotNull { it.max_balance?.toDoubleOrNull() }.sum()
                            val overBudget = totalBudget > 0 && totalSpent > totalBudget

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Monthly Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (totalBudget > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${formatCurrency(totalSpent.toString())} / ${formatCurrency(totalBudget.toString())}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        val progress = (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)
                                        val barColor = when {
                                            overBudget -> MaterialTheme.colorScheme.error
                                            progress > 0.8f -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = barColor,
                                            trackColor = barColor.copy(alpha = 0.2f)
                                        )
                                        Text(
                                            if (overBudget) "Over budget by ${formatCurrency((totalSpent - totalBudget).toString())}"
                                            else "${formatCurrency((totalBudget - totalSpent).toString())} remaining",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (totalGoal > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Sinking funds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${formatCurrency(totalSaved.toString())} / ${formatCurrency(totalGoal.toString())}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        val sinkProgress = (totalSaved / totalGoal).toFloat().coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { sinkProgress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        )
                                    }
                                    if (accountTotals != null) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Debits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(formatCurrency(accountTotals?.debit_total), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Credits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(formatCurrency(accountTotals?.credit_total), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Current Month Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("-${formatCurrency(accountTotals?.current_month_expenses)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            val totalMoney = accountTotals?.total_money?.toDoubleOrNull()
                                            Text(
                                                formatCurrency(accountTotals?.total_money),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (totalMoney != null && totalMoney < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (buckets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No buckets found.")
                                }
                            }
                        } else {
                            items(buckets) { bucket ->
                                val isSinkingFund = bucket.monthly_budget.toDoubleOrNullSafe() == null
                                val monthlySpent = monthlySpentByBucket[bucket.id] ?: 0.0
                                val monthlyDeposits = monthlyDepositsByBucket[bucket.id] ?: 0.0
                                val balanceAtMonthEnd = balanceAtMonthEndByBucket[bucket.id] ?: 0.0
                                BucketCard(
                                    bucket = bucket,
                                    isSinkingFund = isSinkingFund,
                                    monthlySpent = monthlySpent,
                                    monthlyDeposits = monthlyDeposits,
                                    balanceAtMonthEnd = balanceAtMonthEnd,
                                    onClick = { onBucketClick(bucket) }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                false -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please login to view budgets.")
                }
            }
        }
    }
}

@Composable
private fun BucketCard(
    bucket: RemoteBucket,
    isSinkingFund: Boolean,
    monthlySpent: Double,
    monthlyDeposits: Double,
    balanceAtMonthEnd: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    bucket.name ?: "Unnamed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isSinkingFund) {
                    Text(
                        "SINKING FUND",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isSinkingFund) {
                val goal = bucket.max_balance?.toDoubleOrNull()
                val goalReached = goal != null && balanceAtMonthEnd >= goal

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${formatCurrency(balanceAtMonthEnd.toString())} / ${formatCurrency(bucket.max_balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (goalReached) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (goal != null && goal > 0) {
                    val progress = (balanceAtMonthEnd / goal).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                    Text(
                        if (goalReached) "Goal reached!"
                        else "${formatCurrency((goal - balanceAtMonthEnd).toString())} remaining to fill",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (goalReached) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val contribution = bucket.monthly_contribution?.toDoubleOrNull()
                if (contribution != null && contribution > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)
                    val overSpent = monthlySpent > contribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${formatCurrency(monthlySpent.toString())} / ${formatCurrency(bucket.monthly_contribution)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overSpent) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val spentProgress = (monthlySpent / contribution).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { spentProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (overSpent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        trackColor = (if (overSpent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary).copy(alpha = 0.2f)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)
                    val monthFull = monthlyDeposits >= contribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Contributed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${formatCurrency(monthlyDeposits.toString())} / ${formatCurrency(bucket.monthly_contribution)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (monthFull) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val monthProgress = (monthlyDeposits / contribution).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { monthProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                }
            } else {
                val budget = bucket.monthly_budget?.toDoubleOrNull()
                val overBudget = budget != null && monthlySpent > budget

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Spent this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${formatCurrency(monthlySpent.toString())} / ${formatCurrency(bucket.monthly_budget)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (budget != null && budget > 0) {
                    val progress = (monthlySpent / budget).toFloat().coerceIn(0f, 1f)
                    val barColor = when {
                        overBudget -> MaterialTheme.colorScheme.error
                        progress > 0.8f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = barColor,
                        trackColor = barColor.copy(alpha = 0.2f)
                    )
                    Text(
                        if (overBudget)
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

internal fun String?.toDoubleOrNullSafe(): Double? {
    val d = this?.toDoubleOrNull() ?: return null
    return if (d == 0.0) null else d
}

internal fun formatCurrency(value: String?): String {
    val d = value?.toDoubleOrNull() ?: return "—"
    return String.format(Locale.getDefault(), "$%.2f", d)
}
