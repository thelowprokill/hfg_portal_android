package com.jon_is_awesome.hfg_portal_app.ui.carlogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jon_is_awesome.hfg_portal_app.data.model.CarLog
import com.jon_is_awesome.hfg_portal_app.data.repository.AppRepository
import com.jon_is_awesome.hfg_portal_app.worker.MaintenanceReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CarLogViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    val carLogs: StateFlow<List<CarLog>> = repository.getAllCarLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCarLog(carLog: CarLog) {
        viewModelScope.launch {
            repository.insertCarLog(carLog)
            carLog.reminderDate?.let { reminderTime ->
                scheduleReminder(carLog.type, reminderTime)
            }
        }
    }

    private fun scheduleReminder(serviceType: String, reminderTime: Long) {
        val delay = reminderTime - System.currentTimeMillis()
        if (delay > 0) {
            val data = Data.Builder()
                .putString("serviceType", serviceType)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            workManager.enqueue(workRequest)
        }
    }

    fun deleteCarLog(carLog: CarLog) {
        viewModelScope.launch {
            repository.deleteCarLog(carLog)
        }
    }
}

class CarLogViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarLogViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
