package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteCar
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteServiceHistory
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteMaintenanceItem
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CarMaintenanceViewModel(
    application: Application,
    private val remoteDataSource: RemoteDataSource
) : AndroidViewModel(application) {

    val cars: StateFlow<List<RemoteCar>> = remoteDataSource.cars
    val serviceHistory: StateFlow<List<RemoteServiceHistory>> = remoteDataSource.serviceHistory
    val maintenanceItems: StateFlow<List<RemoteMaintenanceItem>> = remoteDataSource.maintenanceItems
    val isLoggedIn: StateFlow<Boolean?> = remoteDataSource.isLoggedIn
    val loginError = remoteDataSource.loginError

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

    fun addServiceHistory(
        carId: Int,
        serviceDate: String,
        mileage: Int,
        name: String,
        maintenanceItemId: Int,
        notes: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = remoteDataSource.addServiceHistory(carId, serviceDate, mileage, name, maintenanceItemId, notes)
            onResult(success)
            if (success) {
                remoteDataSource.fetchServiceHistory(carId)
            }
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
