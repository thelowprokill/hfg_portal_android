package com.jon_is_awesome.hfg_portal_app.data.remote

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import com.jon_is_awesome.hfg_portal_app.BuildConfig

private const val TAG = "RemoteDataSource"

class RemoteDataSource(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _cars = MutableStateFlow<List<RemoteCar>>(emptyList())
    private val _serviceHistory = MutableStateFlow<List<RemoteServiceHistory>>(emptyList())
    private val _maintenanceItems = MutableStateFlow<List<RemoteMaintenanceItem>>(emptyList())
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    private val _token = MutableStateFlow<String?>(null)
    private val _loginError = MutableSharedFlow<String>()
    
    val cars: StateFlow<List<RemoteCar>> = _cars
    val serviceHistory: StateFlow<List<RemoteServiceHistory>> = _serviceHistory
    val maintenanceItems: StateFlow<List<RemoteMaintenanceItem>> = _maintenanceItems
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn
    val loginError = _loginError.asSharedFlow()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val authClient = HttpClient(Android) {
        engine {
            sslManager = { connection ->
                val trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
                connection.sslSocketFactory = sslContext.socketFactory
                connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        // Mimic Postman default behavior
        install(DefaultRequest) {
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.41.1")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
        }
    }

    private var client: HttpClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)
        if (username != null && password != null) {
            login(username, password)
        } else {
            _isLoggedIn.value = false
        }
    }

    fun login(username: String, password: String) {
        // Use regex to replace ALL types of whitespace (including tabs, newlines, non-breaking spaces)
        val cleanUsername = username.replace("\\s".toRegex(), "")
        val cleanPassword = password.replace("\\s".toRegex(), "")
        
        _isLoggedIn.value = null
        scope.launch {
            try {
                val baseURL = BuildConfig.BASE_URL
                Log.d(TAG, "Login Attempt: user='$cleanUsername', passLen=${cleanPassword.length}")
                
                val httpResponse = authClient.get("${baseURL}/apis/call") {
                    parameter("username", cleanUsername)
                    parameter("password", cleanPassword)
                }
                
                // Detailed logging to compare with Postman
                Log.d(TAG, "Actual Request URL: ${httpResponse.request.url}")
                Log.d(TAG, "Request Headers: ${httpResponse.request.headers.entries()}")
                
                val bodyText = httpResponse.bodyAsText()
                Log.d(TAG, "Raw Login Response Body: $bodyText")
                
                val response = json.decodeFromString<AuthResponse>(bodyText)
                Log.d(TAG, "Parsed Login Response: jwt=${response.jwt}, username=${response.username}")
                
                val token = response.jwt
                if (!token.isNullOrBlank()) {
                    _token.value = token
                    sharedPreferences.edit().apply {
                        putString("username", cleanUsername)
                        putString("password", cleanPassword)
                        apply()
                    }
                    initClient(token)
                    _isLoggedIn.value = true
                    fetchCars()
                } else {
                    Log.w(TAG, "Login failed: jwt is null or blank in response")
                    _isLoggedIn.value = false
                    _loginError.emit("Login failed. Please check your credentials.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error during login: ${e.message}", e)
                _isLoggedIn.value = false
                _loginError.emit("Network error: ${e.message}")
            }
        }
    }

    private fun initClient(token: String) {
        client = HttpClient(Android) {
            engine {
                sslManager = { connection ->
                    val trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(DefaultRequest) {
                header("x-api-key", token)
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.41.1")
            }
        }
    }

    suspend fun fetchCars() {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: CarResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "car_maintenance:cars")
                    parameter("route", "cars")
                }.body()
                _cars.value = response.data
                Log.d(TAG, "fetchCars success: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchCars error", e)
            }
        }
    }

    suspend fun fetchServiceHistory(carId: Int) {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: ServiceHistoryResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "car_maintenance:service_history")
                    parameter("route", "service_histories")
                    parameter("car_id", carId)
                }.body()
                _serviceHistory.value = response.data
                Log.d(TAG, "fetchServiceHistory success for car $carId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchServiceHistory error", e)
            }
        }
    }

    suspend fun fetchMaintenanceItems(carId: Int) {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: MaintenanceItemsResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "car_maintenance:maintenance_items")
                    parameter("route", "maintenance_items")
                    parameter("car_id", carId)
                }.body()
                _maintenanceItems.value = response.data
                Log.d(TAG, "fetchMaintenanceItems success for car $carId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchMaintenanceItems error", e)
            }
        }
    }

    suspend fun addServiceHistory(
        carId: Int,
        serviceDate: String,
        mileage: Int,
        name: String,
        maintenanceItemId: Int,
        notes: String
    ): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.put("${baseURL}/apis/call") {
                    parameter("scope", "car_maintenance:service_history")
                    parameter("route", "service_history")
                    parameter("car_id", carId)
                    parameter("service_date", serviceDate)
                    parameter("mileage", mileage)
                    parameter("name", name)
                    parameter("maintenance_item_id", maintenanceItemId)
                    parameter("notes", notes)
                }.body()
                Log.d(TAG, "addServiceHistory response: ${response.response}")
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "addServiceHistory error", e)
                false
            }
        } ?: false
    }

    fun logout() {
        Log.d(TAG, "logout called")
        sharedPreferences.edit().clear().apply()
        _token.value = null
        _isLoggedIn.value = false
        _cars.value = emptyList()
        client?.close()
        client = null
    }
}
