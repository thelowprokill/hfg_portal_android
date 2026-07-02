package com.jon_is_awesome.hfg_portal_app.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteBucket
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteTransaction
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteScheduledExpense
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetRemoteViewModel(
    application: Application,
    private val remoteDataSource: RemoteDataSource
) : AndroidViewModel(application) {

    val buckets: StateFlow<List<RemoteBucket>> = remoteDataSource.buckets
    val transactions: StateFlow<List<RemoteTransaction>> = remoteDataSource.transactions
    val allTransactions: StateFlow<List<RemoteTransaction>> = remoteDataSource.allTransactions
    val scheduledExpenses: StateFlow<List<RemoteScheduledExpense>> = remoteDataSource.scheduledExpenses
    val isLoggedIn: StateFlow<Boolean?> = remoteDataSource.isLoggedIn
    val userRoles: StateFlow<Map<String, String>> = remoteDataSource.userRoles

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortField = MutableStateFlow("name")
    val sortField: StateFlow<String> = _sortField

    private val _sortOrder = MutableStateFlow("asc")
    val sortOrder: StateFlow<String> = _sortOrder

    private val _selectedMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    fun setSelectedMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    private var searchJob: Job? = null

    private fun doFetchBuckets() {
        viewModelScope.launch {
            remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
        }
    }

    fun fetchBuckets() = doFetchBuckets()

    fun setSearch(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
        }
    }

    fun setSort(field: String, order: String) {
        _sortField.value = field
        _sortOrder.value = order
        doFetchBuckets()
    }

    fun fetchAllTransactions() {
        viewModelScope.launch { remoteDataSource.fetchAllTransactions() }
    }

    suspend fun refreshBudgetList() {
        coroutineScope {
            launch { remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value) }
            launch { remoteDataSource.fetchAllTransactions() }
        }
    }

    suspend fun refreshBucketDetail(bucketId: Int) {
        remoteDataSource.fetchTransactions(bucketId)
        remoteDataSource.fetchScheduledExpenses(bucketId)
    }

    fun fetchBucketDetail(bucketId: Int) {
        viewModelScope.launch {
            remoteDataSource.fetchTransactions(bucketId)
            remoteDataSource.fetchScheduledExpenses(bucketId)
        }
    }

    fun addBucket(
        name: String,
        description: String,
        monthlyBudget: String,
        monthlyContribution: String,
        maxBalance: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = remoteDataSource.addBucket(name, description, monthlyBudget, monthlyContribution, maxBalance)
            if (success) remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
            onResult(success)
        }
    }

    fun deleteBucket(id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = remoteDataSource.deleteBucket(id)
            if (success) remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
            onResult(success)
        }
    }

    fun addTransaction(
        bucketId: Int,
        amount: String,
        description: String,
        date: String,
        type: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = remoteDataSource.addTransaction(bucketId, amount, description, date, type)
            if (success) {
                remoteDataSource.fetchTransactions(bucketId)
                remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
                remoteDataSource.fetchAllTransactions()
            }
            onResult(success)
        }
    }

    fun deleteTransaction(id: Int, bucketId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = remoteDataSource.deleteTransaction(id)
            if (success) {
                remoteDataSource.fetchTransactions(bucketId)
                remoteDataSource.fetchBuckets(_searchQuery.value, _sortField.value, _sortOrder.value)
                remoteDataSource.fetchAllTransactions()
            }
            onResult(success)
        }
    }
}

class BudgetRemoteViewModelFactory(
    private val application: Application,
    private val remoteDataSource: RemoteDataSource
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetRemoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetRemoteViewModel(application, remoteDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
