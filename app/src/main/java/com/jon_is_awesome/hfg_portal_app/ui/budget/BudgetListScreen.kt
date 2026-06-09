package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteBucket
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    viewModel: BudgetRemoteViewModel,
    onAddBucketClick: () -> Unit,
    onBucketClick: (RemoteBucket) -> Unit
) {
    val buckets by viewModel.buckets.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Budget") })
        },
        floatingActionButton = {
            if (isLoggedIn == true) {
                FloatingActionButton(onClick = onAddBucketClick) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Bucket")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (isLoggedIn) {
                true -> {
                    if (buckets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No buckets found.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                    Text("Balance", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Limit/Goal", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider()
                            }
                            items(buckets) { bucket ->
                                val isSinkingFund = bucket.monthly_budget.toDoubleOrNullSafe() == null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onBucketClick(bucket) }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(bucket.name ?: "Unnamed", modifier = Modifier.weight(2f))
                                    Text(
                                        formatCurrency(bucket.balance),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (isSinkingFund) formatCurrency(bucket.max_balance)
                                        else formatCurrency(bucket.monthly_budget),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp)
                            }
                        }
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

internal fun String?.toDoubleOrNullSafe(): Double? {
    val d = this?.toDoubleOrNull() ?: return null
    return if (d == 0.0) null else d
}

internal fun formatCurrency(value: String?): String {
    val d = value?.toDoubleOrNull() ?: return "—"
    return String.format(Locale.getDefault(), "$%.2f", d)
}
