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
import kotlinx.coroutines.coroutineScope
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

    fun fetchBuckets() {
        viewModelScope.launch { remoteDataSource.fetchBuckets() }
    }

    fun fetchAllTransactions() {
        viewModelScope.launch { remoteDataSource.fetchAllTransactions() }
    }

    suspend fun refreshBudgetList() {
        coroutineScope {
            launch { remoteDataSource.fetchBuckets() }
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
            if (success) remoteDataSource.fetchBuckets()
            onResult(success)
        }
    }

    fun deleteBucket(id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = remoteDataSource.deleteBucket(id)
            if (success) remoteDataSource.fetchBuckets()
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
                remoteDataSource.fetchBuckets()
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
                remoteDataSource.fetchBuckets()
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
