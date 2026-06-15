package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import androidx.compose.foundation.clickable
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
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteCar
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteServiceHistory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarLogListScreen(
    viewModel: CarLogViewModel,
    remoteViewModel: CarMaintenanceViewModel,
    onAddLogClick: () -> Unit,
    onServiceHistoryClick: (RemoteServiceHistory) -> Unit
) {
    val cars by remoteViewModel.cars.collectAsState()
    val serviceHistory by remoteViewModel.serviceHistory.collectAsState()
    val isLoggedIn by remoteViewModel.isLoggedIn.collectAsState()
    val selectedCar by remoteViewModel.selectedCar.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Text(if (selectedCar != null) "Service History: ${selectedCar?.name}" else "Car Maintenance")
                },
                navigationIcon = {
                    if (selectedCar != null) {
                        IconButton(onClick = { remoteViewModel.selectCar(null) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCar != null) {
                FloatingActionButton(onClick = onAddLogClick) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Service History")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoggedIn == true) {
                if (selectedCar == null) {
                    CarList(
                        cars = cars,
                        onCarClick = { car ->
                            remoteViewModel.selectCar(car)
                            remoteViewModel.fetchServiceHistory(car.id)
                        }
                    )
                } else {
                    ServiceHistoryList(
                        history = serviceHistory,
                        onItemClick = onServiceHistoryClick
                    )
                }
            } else if (isLoggedIn == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please login to view car maintenance data.")
                }
            }
        }
    }
}

@Composable
fun CarList(cars: List<RemoteCar>, onCarClick: (RemoteCar) -> Unit) {
    if (cars.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cars found.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Year", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                    Text("Make", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Model", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
            items(cars) { car ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCarClick(car) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(car.name ?: "Unknown", modifier = Modifier.weight(1f))
                    Text(car.year?.toString() ?: "-", modifier = Modifier.weight(0.5f))
                    Text(car.make ?: "-", modifier = Modifier.weight(1f))
                    Text(car.model ?: "-", modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ServiceHistoryList(
    history: List<RemoteServiceHistory>,
    onItemClick: (RemoteServiceHistory) -> Unit
) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No service history found.")
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
                    Text("Service", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Miles", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                    Text("Next", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
            items(history) { entry ->
                val nextService = entry.next_service?.let {
                    if (it.toString() == "null") "-" else it.toString().trim('"')
                } ?: "-"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(entry) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.item ?: "Unknown", modifier = Modifier.weight(2f))
                    Text(entry.service_date ?: "-", modifier = Modifier.weight(1f))
                    Text(entry.mileage?.toString() ?: "-", modifier = Modifier.weight(0.8f))
                    Text(nextService, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun CarLogItem(
    log: CarLog,
    onDeleteClick: (CarLog) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(log.date),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.getDefault(), "Cost: $%.2f", log.cost),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Mileage: ${log.mileage}",
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
