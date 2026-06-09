package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteMaintenanceImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryDetailScreen(
    serviceHistoryId: Int,
    remoteViewModel: CarMaintenanceViewModel,
    onNavigateBack: () -> Unit
) {
    val serviceHistory by remoteViewModel.serviceHistory.collectAsState()
    val maintenanceImages by remoteViewModel.maintenanceImages.collectAsState()
    val context = LocalContext.current

    val entry = serviceHistory.find { it.id == serviceHistoryId }

    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(serviceHistoryId) {
        remoteViewModel.fetchMaintenanceImages(serviceHistoryId)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            uploadError = null
            remoteViewModel.addMaintenanceImageFromUri(context, uri, serviceHistoryId) { success ->
                isUploading = false
                if (!success) uploadError = "Failed to upload image. Please try again."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.item ?: "Service Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Image")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            entry?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(it.item ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${it.service_date ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                        Text("Mileage: ${it.mileage ?: 0}", style = MaterialTheme.typography.bodySmall)
                        if (!it.notes.isNullOrBlank()) {
                            Text("Notes: ${it.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            uploadError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Photos",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (maintenanceImages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photos yet. Tap + to add one.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(maintenanceImages) { img ->
                        MaintenanceImageItem(img)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceImageItem(img: RemoteMaintenanceImage) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(img.image) {
        bitmap = withContext(Dispatchers.IO) {
            img.image?.let { encoded ->
                try {
                    // Strip "data:image/...;base64," prefix if present
                    val base64Data = if (encoded.contains(",")) encoded.substringAfter(",") else encoded
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = img.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
