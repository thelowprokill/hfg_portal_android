package com.jon_is_awesome.hfg_portal_app.ui.loanrepayment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteLoan
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteRepayment
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoanRepaymentViewModel(
    application: Application,
    private val remoteDataSource: RemoteDataSource
) : AndroidViewModel(application) {

    val loans: StateFlow<List<RemoteLoan>> = remoteDataSource.loans
    val repayments: StateFlow<List<RemoteRepayment>> = remoteDataSource.repayments
    val isLoggedIn: StateFlow<Boolean?> = remoteDataSource.isLoggedIn
    val userRoles: StateFlow<Map<String, String>> = remoteDataSource.userRoles

    fun fetchLoans() {
        viewModelScope.launch { remoteDataSource.fetchLoans() }
    }

    suspend fun refreshLoanList() {
        remoteDataSource.fetchLoans()
    }

    fun fetchRepayments(loanId: Int) {
        viewModelScope.launch { remoteDataSource.fetchRepayments(loanId) }
    }

    suspend fun refreshLoanDetail(loanId: Int) {
        remoteDataSource.fetchRepayments(loanId)
    }

    fun addRepayment(
        loanId: Int,
        paidAmount: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = remoteDataSource.addRepayment(loanId, paidAmount)
            if (success) {
                remoteDataSource.fetchRepayments(loanId)
                remoteDataSource.fetchLoans()
            }
            onResult(success)
        }
    }

    fun deleteRepayment(id: Int, loanId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = remoteDataSource.deleteRepayment(id)
            if (success) remoteDataSource.fetchRepayments(loanId)
            onResult(success)
        }
    }
}

class LoanRepaymentViewModelFactory(
    private val application: Application,
    private val remoteDataSource: RemoteDataSource
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanRepaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoanRepaymentViewModel(application, remoteDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
