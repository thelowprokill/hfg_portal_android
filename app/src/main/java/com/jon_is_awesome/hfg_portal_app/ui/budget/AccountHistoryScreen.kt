package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteAccountHistoryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ISO_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private data class Series(val label: String, val color: Color, val select: (RemoteAccountHistoryPoint) -> String?)

// Fixed series colors — distinct and legible in both light and dark themes.
private val SERIES = listOf(
    Series("Debit", Color(0xFF4285F4)) { it.debit_total },
    Series("Credit", Color(0xFF34A853)) { it.credit_total },
    Series("Sinking", Color(0xFFFBBC05)) { it.sinking_total },
    Series("Emergency", Color(0xFFEA4335)) { it.emergency_total },
    Series("Total", Color(0xFF9C27B0)) { it.total_money },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHistoryScreen(
    viewModel: BudgetRemoteViewModel,
    onNavigateBack: () -> Unit
) {
    var interval by remember { mutableStateOf("day") }
    var fromDate by remember { mutableStateOf<String?>(null) }
    var toDate by remember { mutableStateOf<String?>(null) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val history by viewModel.accountHistory.collectAsState()

    LaunchedEffect(interval, fromDate, toDate) { viewModel.fetchAccountHistory(interval, fromDate, toDate) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("day" to "Daily", "month" to "Monthly").forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = interval == value,
                        onClick = { interval = value },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                    ) { Text(label) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) {
                    Text(fromDate ?: "From: 6mo ago")
                }
                OutlinedButton(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) {
                    Text(toDate ?: "To: Today")
                }
                if (fromDate != null || toDate != null) {
                    IconButton(onClick = { fromDate = null; toDate = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Reset date range")
                    }
                }
            }

            if (showFromPicker) {
                DateRangePickerDialog(
                    initialIsoDate = fromDate,
                    onDismiss = { showFromPicker = false },
                    onConfirm = { fromDate = it; showFromPicker = false }
                )
            }
            if (showToPicker) {
                DateRangePickerDialog(
                    initialIsoDate = toDate,
                    onDismiss = { showToPicker = false },
                    onConfirm = { toDate = it; showToPicker = false }
                )
            }

            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No history available.")
                }
            } else {
                LineChart(history, modifier = Modifier.fillMaxWidth().weight(1f))
                Legend()
            }
        }
    }
}

@Composable
private fun LineChart(points: List<RemoteAccountHistoryPoint>, modifier: Modifier = Modifier) {
    val values = SERIES.map { (_, _, sel) -> points.map { sel(it)?.toDoubleOrNull() } }
    val allValues = values.flatten().filterNotNull()
    if (allValues.isEmpty()) return
    val min = allValues.min()
    val max = allValues.max()
    val range = (max - min).takeIf { it != 0.0 } ?: 1.0
    val outline = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier) {
        val h = size.height
        val w = size.width
        // Baseline axis
        drawLine(outline, Offset(0f, h), Offset(w, h), strokeWidth = 2f)

        fun x(i: Int) = if (points.size <= 1) 0f else w * i / (points.size - 1)
        fun y(v: Double) = (h - (((v - min) / range) * h)).toFloat()

        values.forEachIndexed { s, seriesVals ->
            val color = SERIES[s].color
            var prev: Offset? = null
            seriesVals.forEachIndexed { i, v ->
                val cur = if (v == null) null else Offset(x(i), y(v))
                if (prev != null && cur != null) drawLine(color, prev!!, cur, strokeWidth = 4f)
                prev = cur ?: prev
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialIsoDate: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialIsoDate?.let { ISO_DATE.parse(it)?.time }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(ISO_DATE.format(java.util.Date(it))) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SERIES.forEach { (label, color, _) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(Modifier.size(12.dp)) { drawCircle(color) }
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}
