package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteCar
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteMaintenanceImage
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteMaintenanceItem
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteServiceHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarMaintenanceViewModel(
    application: Application,
    private val remoteDataSource: RemoteDataSource
) : AndroidViewModel(application) {

    val cars: StateFlow<List<RemoteCar>> = remoteDataSource.cars
    val serviceHistory: StateFlow<List<RemoteServiceHistory>> = remoteDataSource.serviceHistory
    val maintenanceItems: StateFlow<List<RemoteMaintenanceItem>> = remoteDataSource.maintenanceItems
    val maintenanceImages: StateFlow<List<RemoteMaintenanceImage>> = remoteDataSource.maintenanceImages
    val isLoggedIn: StateFlow<Boolean?> = remoteDataSource.isLoggedIn
    val loginError = remoteDataSource.loginError

    private val _selectedCar = MutableStateFlow<RemoteCar?>(null)
    val selectedCar: StateFlow<RemoteCar?> = _selectedCar

    fun selectCar(car: RemoteCar?) {
        _selectedCar.value = car
        if (car != null) {
            viewModelScope.launch {
                remoteDataSource.fetchMaintenanceItems(car.id)
            }
        }
    }

    fun login(username: String, password: String) {
        remoteDataSource.login(username, password)
    }

    fun logout() {
        remoteDataSource.logout()
    }

    fun fetchCars() {
        viewModelScope.launch {
            remoteDataSource.fetchCars()
        }
    }

    fun fetchServiceHistory(carId: Int) {
        viewModelScope.launch {
            remoteDataSource.fetchServiceHistory(carId)
        }
    }

    fun fetchMaintenanceItems(carId: Int) {
        viewModelScope.launch {
            remoteDataSource.fetchMaintenanceItems(carId)
        }
    }

    fun fetchMaintenanceImages(serviceHistoryId: Int) {
        viewModelScope.launch {
            remoteDataSource.fetchMaintenanceImages(serviceHistoryId)
        }
    }

    fun addServiceHistory(
        carId: Int,
        serviceDate: String,
        mileage: Int,
        name: String,
        maintenanceItemId: Int,
        namedItem: String,
        notes: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = remoteDataSource.addServiceHistory(
                carId, serviceDate, mileage, name, maintenanceItemId, namedItem, notes
            )
            onResult(success)
            if (success) {
                remoteDataSource.fetchServiceHistory(carId)
            }
        }
    }

    fun addMaintenanceImageFromUri(
        context: Context,
        uri: Uri,
        serviceHistoryId: Int,
        name: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: return@withContext false
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext false
                val maxDim = 1024
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    android.graphics.Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else bitmap
                val out = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                remoteDataSource.addMaintenanceImage(serviceHistoryId, name, out.toByteArray(), "image/jpeg")
            }
            if (success) {
                remoteDataSource.fetchMaintenanceImages(serviceHistoryId)
            }
            onResult(success)
        }
    }
}

class CarMaintenanceViewModelFactory(
    private val application: Application,
    private val remoteDataSource: RemoteDataSource
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarMaintenanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarMaintenanceViewModel(application, remoteDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
