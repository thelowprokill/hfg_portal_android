package com.jon_is_awesome.hfg_portal_app.ui.carlogs

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarLogListScreen(
    viewModel: CarLogViewModel,
    onAddLogClick: () -> Unit
) {
    val carLogs by viewModel.carLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Car Maintenance Logs") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLogClick) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Log")
            }
        }
    ) { padding ->
        if (carLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No logs found. Add your first service record!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(carLogs) { log ->
                    CarLogItem(
                        log = log,
                        onDeleteClick = { viewModel.deleteCarLog(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CarLogItem(
    log: CarLog,
    onDeleteClick: (CarLog) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${formatDate(log.date)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Cost: $${log.cost}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (log.notes.isNotEmpty()) {
                    Text(
                        text = "Notes: ${log.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
                Text(
                    text = "Mileage: ${log.mileage} miles",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onDeleteClick(log) }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
