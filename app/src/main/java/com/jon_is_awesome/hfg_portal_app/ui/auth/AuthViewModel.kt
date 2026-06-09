package com.jon_is_awesome.hfg_portal_app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val remoteDataSource: RemoteDataSource) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean?> = remoteDataSource.isLoggedIn
    val loginError = remoteDataSource.loginError

    fun login(username: String, password: String) {
        remoteDataSource.login(username, password)
    }

    fun logout() {
        remoteDataSource.logout()
    }
}

class AuthViewModelFactory(private val remoteDataSource: RemoteDataSource) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(remoteDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
