package com.jon_is_awesome.hfg_portal_app.ui.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteAccountHistoryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

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
                var selected by remember(history) { mutableStateOf<Int?>(null) }
                LineChart(
                    history,
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Legend(selected?.let(history::getOrNull))
            }
        }
    }
}

// Round gridline values (multiples of 1/2/5 x 10^n) spanning [min, max].
internal fun niceTicks(min: Double, max: Double, target: Int = 4): List<Double> {
    val pad = if (max > min) 0.0 else maxOf(abs(min) * 0.1, 1.0)
    val lo = min - pad
    val hi = max + pad
    val raw = (hi - lo) / target
    val mag = 10.0.pow(floor(log10(raw)))
    val step = listOf(1.0, 2.0, 5.0, 10.0).map { it * mag }.first { it >= raw }
    val first = floor(lo / step)
    val last = ceil(hi / step)
    return (0..(last - first).toInt()).map { (first + it) * step }
}

// Ticks are exact multiples of the step, so scaling by the step's unit never needs decimals.
internal fun tickLabel(v: Double, step: Double): String {
    val (unit, suffix) = when {
        step >= 1e6 -> 1e6 to "M"
        step >= 1e3 -> 1e3 to "k"
        else -> 1.0 to ""
    }
    val sign = if (v < 0) "-" else ""
    return "$sign$" + "%,.0f".format(abs(v) / unit) + suffix
}

@Composable
private fun LineChart(
    points: List<RemoteAccountHistoryPoint>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val values = SERIES.map { (_, _, sel) -> points.map { sel(it)?.toDoubleOrNull() } }
    val allValues = values.flatten().filterNotNull()
    if (allValues.isEmpty()) return
    val ticks = niceTicks(allValues.min(), allValues.max())
    val min = ticks.first()
    val range = ticks.last() - min
    val step = ticks[1] - ticks[0]

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cursorColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val measurer = rememberTextMeasurer()
    val labels = remember(ticks, labelStyle) { ticks.map { measurer.measure(tickLabel(it, step), labelStyle) } }
    val gutter = labels.maxOf { it.size.width } + 12f
    val labelHalfH = labels.first().size.height / 2f

    fun indexAt(x: Float, width: Float): Int {
        if (points.size <= 1) return 0
        val frac = ((x - gutter) / (width - gutter)).coerceIn(0f, 1f)
        return (frac * (points.size - 1)).roundToInt()
    }

    Canvas(
        modifier
            .pointerInput(points, gutter) {
                detectTapGestures { onSelect(indexAt(it.x, size.width.toFloat())) }
            }
            .pointerInput(points, gutter) {
                detectHorizontalDragGestures(
                    onDragStart = { onSelect(indexAt(it.x, size.width.toFloat())) }
                ) { change, _ -> onSelect(indexAt(change.position.x, size.width.toFloat())) }
            }
    ) {
        val top = labelHalfH
        val bottom = size.height - labelHalfH
        val plotW = size.width - gutter

        fun x(i: Int) = gutter + if (points.size <= 1) 0f else plotW * i / (points.size - 1)
        fun y(v: Double) = (bottom - ((v - min) / range) * (bottom - top)).toFloat()

        ticks.forEachIndexed { i, t ->
            val ty = y(t)
            drawLine(gridColor, Offset(gutter, ty), Offset(size.width, ty), strokeWidth = 1.5f)
            drawText(labels[i], topLeft = Offset(0f, ty - labelHalfH))
        }

        values.forEachIndexed { s, seriesVals ->
            val color = SERIES[s].color
            var prev: Offset? = null
            seriesVals.forEachIndexed { i, v ->
                val cur = if (v == null) null else Offset(x(i), y(v))
                if (prev != null && cur != null) drawLine(color, prev!!, cur, strokeWidth = 4f)
                prev = cur ?: prev
            }
        }

        selected?.takeIf { it in points.indices }?.let { i ->
            drawLine(cursorColor, Offset(x(i), top), Offset(x(i), bottom), strokeWidth = 2f)
            values.forEachIndexed { s, seriesVals ->
                seriesVals[i]?.let { drawCircle(SERIES[s].color, radius = 8f, center = Offset(x(i), y(it))) }
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
private fun Legend(point: RemoteAccountHistoryPoint?) {
    Text(
        point?.date ?: "Drag across the chart to see values",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SERIES.forEach { (label, color, select) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(Modifier.size(12.dp)) { drawCircle(color) }
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                point?.let { select(it)?.toDoubleOrNull() }?.let {
                    Text("$%,.2f".format(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
