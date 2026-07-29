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
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
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
    private val _maintenanceImages = MutableStateFlow<List<RemoteMaintenanceImage>>(emptyList())
    private val _loans = MutableStateFlow<List<RemoteLoan>>(emptyList())
    private val _repayments = MutableStateFlow<List<RemoteRepayment>>(emptyList())
    private val _buckets = MutableStateFlow<List<RemoteBucket>>(emptyList())
    private val _transactions = MutableStateFlow<List<RemoteTransaction>>(emptyList())
    private val _allTransactions = MutableStateFlow<List<RemoteTransaction>>(emptyList())
    private val _scheduledExpenses = MutableStateFlow<List<RemoteScheduledExpense>>(emptyList())
    private val _accountTotals = MutableStateFlow<RemoteAccountTotals?>(null)
    private val _accountHistory = MutableStateFlow<List<RemoteAccountHistoryPoint>>(emptyList())
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    private val _token = MutableStateFlow<String?>(null)
    private val _loginError = MutableSharedFlow<String>()
    private val _loggedInUsername = MutableStateFlow<String?>(null)
    private val _userRoles = MutableStateFlow<Map<String, String>>(emptyMap())

    val loans: StateFlow<List<RemoteLoan>> = _loans
    val repayments: StateFlow<List<RemoteRepayment>> = _repayments
    val cars: StateFlow<List<RemoteCar>> = _cars
    val serviceHistory: StateFlow<List<RemoteServiceHistory>> = _serviceHistory
    val maintenanceItems: StateFlow<List<RemoteMaintenanceItem>> = _maintenanceItems
    val maintenanceImages: StateFlow<List<RemoteMaintenanceImage>> = _maintenanceImages
    val buckets: StateFlow<List<RemoteBucket>> = _buckets
    val transactions: StateFlow<List<RemoteTransaction>> = _transactions
    val allTransactions: StateFlow<List<RemoteTransaction>> = _allTransactions
    val scheduledExpenses: StateFlow<List<RemoteScheduledExpense>> = _scheduledExpenses
    val accountTotals: StateFlow<RemoteAccountTotals?> = _accountTotals
    val accountHistory: StateFlow<List<RemoteAccountHistoryPoint>> = _accountHistory
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn
    val loginError = _loginError.asSharedFlow()
    val loggedInUsername: StateFlow<String?> = _loggedInUsername
    val userRoles: StateFlow<Map<String, String>> = _userRoles

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error creating EncryptedSharedPreferences", e)
        context.deleteSharedPreferences("secret_shared_prefs")
        EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
                //Log.d(TAG, "Actual Request URL: ${httpResponse.request.url}")
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
                    _loggedInUsername.value = response.username
                    _userRoles.value = response.apps
                        .filter { it.app != null && it.role != null }
                        .associate { it.app!! to it.role!! }
                    initClient(token)
                    _isLoggedIn.value = true
                    fetchCars()
                    fetchBuckets()
                    fetchAllTransactions()
                    val now = java.util.Calendar.getInstance()
                    fetchAccountTotals(now.get(java.util.Calendar.MONTH) + 1, now.get(java.util.Calendar.YEAR))
                    fetchLoans()
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
        maintenanceItemId: Int?,
        namedItem: String,
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
                    if (maintenanceItemId != null) parameter("item_id", maintenanceItemId)
                    parameter("named_item", namedItem)
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

    suspend fun fetchMaintenanceImages(serviceHistoryId: Int) {
        val baseURL = BuildConfig.BASE_URL
        _maintenanceImages.value = emptyList()
        client?.let {
            try {
                val response: MaintenanceImagesResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "car_maintenance:maintenance_images")
                    parameter("route", "maintenance_images")
                    parameter("service_history_id", serviceHistoryId)
                }.body()
                _maintenanceImages.value = response.data
                Log.d(TAG, "fetchMaintenanceImages success for history $serviceHistoryId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchMaintenanceImages error", e)
            }
        }
    }

    suspend fun addMaintenanceImage(
        serviceHistoryId: Int,
        name: String,
        imageBytes: ByteArray
    ): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.submitFormWithBinaryData(
                    url = "${baseURL}/apis/call",
                    formData = formData {
                        append("service_history_id", serviceHistoryId.toString())
                        append("name", name)
                        append("image", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                        })
                    }
                ) {
                    method = HttpMethod.Put
                    url {
                        parameters.append("scope", "car_maintenance:maintenance_images")
                        parameters.append("route", "maintenance_image")
                    }
                }.body()
                Log.d(TAG, "addMaintenanceImage response: ${response.response}")
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "addMaintenanceImage error", e)
                false
            }
        } ?: false
    }

    suspend fun fetchLoans() {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: LoanListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "loan_repayment:loans")
                    parameter("route", "loans")
                }.body()
                _loans.value = response.data
                Log.d(TAG, "fetchLoans success: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchLoans error", e)
            }
        }
    }

    suspend fun fetchRepayments(loanId: Int) {
        val baseURL = BuildConfig.BASE_URL
        _repayments.value = emptyList()
        client?.let {
            try {
                val response: RepaymentListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "loan_repayment:repayments")
                    parameter("route", "repayments")
                    parameter("loan_id", loanId)
                }.body()
                _repayments.value = response.data
                Log.d(TAG, "fetchRepayments success for loan $loanId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchRepayments error", e)
            }
        }
    }

    suspend fun addRepayment(
        loanId: Int,
        paidAmount: String
    ): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.put("${baseURL}/apis/call") {
                    parameter("scope", "loan_repayment:repayments")
                    parameter("route", "repayment")
                    parameter("loan_id", loanId)
                    parameter("paid_amount", paidAmount)
                }.body()
                Log.d(TAG, "addRepayment response: ${response.response}")
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "addRepayment error", e)
                false
            }
        } ?: false
    }

    suspend fun deleteRepayment(id: Int): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.delete("${baseURL}/apis/call") {
                    parameter("scope", "loan_repayment:repayments")
                    parameter("route", "repayment")
                    parameter("id", id)
                }.body()
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "deleteRepayment error", e)
                false
            }
        } ?: false
    }

    suspend fun fetchBuckets(search: String = "", sortField: String = "name", sortOrder: String = "asc") {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: BucketListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget:buckets")
                    parameter("route", "buckets")
                    if (search.isNotBlank()) {
                        parameter("search_stack", "[{\"field\":\"name\",\"val\":\"$search\"}]")
                    }
                    parameter("sort_stack", "[{\"field\":\"$sortField\",\"order\":\"$sortOrder\"}]")
                }.body()
                _buckets.value = response.data
                Log.d(TAG, "fetchBuckets success: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchBuckets error", e)
            }
        }
    }

    suspend fun fetchTransactions(bucketId: Int) {
        val baseURL = BuildConfig.BASE_URL
        _transactions.value = emptyList()
        client?.let {
            try {
                val response: TransactionListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget:transactions")
                    parameter("route", "transactions")
                    parameter("bucket_id", bucketId)
                }.body()
                _transactions.value = response.data
                Log.d(TAG, "fetchTransactions success for bucket $bucketId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchTransactions error", e)
            }
        }
    }

    suspend fun fetchAllTransactions() {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: TransactionListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget:transactions")
                    parameter("route", "transactions")
                }.body()
                _allTransactions.value = response.data
                Log.d(TAG, "fetchAllTransactions success: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchAllTransactions error", e)
            }
        }
    }

    suspend fun fetchAccountTotals(month: Int, year: Int) {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: AccountTotalsResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget")
                    parameter("route", "account_totals")
                    parameter("month", month)
                    parameter("year", year)
                }.body()
                _accountTotals.value = response.data
                Log.d(TAG, "fetchAccountTotals success for $month/$year")
            } catch (e: Exception) {
                Log.e(TAG, "fetchAccountTotals error", e)
            }
        }
    }

    suspend fun fetchAccountHistory(from: String, to: String, interval: String) {
        val baseURL = BuildConfig.BASE_URL
        client?.let {
            try {
                val response: AccountHistoryResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget")
                    parameter("route", "account_history")
                    parameter("from", from)
                    parameter("to", to)
                    parameter("interval", interval)
                }.body()
                _accountHistory.value = response.data.points
                Log.d(TAG, "fetchAccountHistory success: count=${response.data.points.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchAccountHistory error", e)
            }
        }
    }

    suspend fun fetchScheduledExpenses(bucketId: Int) {
        val baseURL = BuildConfig.BASE_URL
        _scheduledExpenses.value = emptyList()
        client?.let {
            try {
                val response: ScheduledExpenseListResponse = it.get("${baseURL}/apis/call") {
                    parameter("scope", "budget:scheduled_expenses")
                    parameter("route", "scheduled_expenses")
                    parameter("bucket_id", bucketId)
                }.body()
                _scheduledExpenses.value = response.data
                Log.d(TAG, "fetchScheduledExpenses success for bucket $bucketId: count=${response.data.size}")
            } catch (e: Exception) {
                Log.e(TAG, "fetchScheduledExpenses error", e)
            }
        }
    }

    suspend fun addBucket(
        name: String,
        description: String,
        monthlyBudget: String,
        monthlyContribution: String,
        maxBalance: String
    ): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.put("${baseURL}/apis/call") {
                    parameter("scope", "budget:buckets")
                    parameter("route", "bucket")
                    parameter("name", name)
                    if (description.isNotBlank()) parameter("description", description)
                    if (monthlyBudget.isNotBlank()) parameter("monthly_budget", monthlyBudget)
                    if (monthlyContribution.isNotBlank()) parameter("monthly_contribution", monthlyContribution)
                    if (maxBalance.isNotBlank()) parameter("max_balance", maxBalance)
                }.body()
                Log.d(TAG, "addBucket response: ${response.response}")
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "addBucket error", e)
                false
            }
        } ?: false
    }

    suspend fun deleteBucket(id: Int): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.delete("${baseURL}/apis/call") {
                    parameter("scope", "budget:buckets")
                    parameter("route", "bucket")
                    parameter("id", id)
                }.body()
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "deleteBucket error", e)
                false
            }
        } ?: false
    }

    suspend fun addTransaction(
        bucketId: Int,
        amount: String,
        description: String,
        transactionDate: String,
        type: String,
        id: Int? = null
    ): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val httpResponse = it.put("${baseURL}/apis/call") {
                    parameter("scope", "budget:transactions")
                    parameter("route", "transaction")
                    if (id != null) parameter("id", id)
                    parameter("bucket_id", bucketId)
                    parameter("amount", amount)
                    parameter("description", description)
                    parameter("transaction_date", transactionDate)
                    parameter("type", type)
                }
                Log.d(TAG, "addTransaction URL: ${httpResponse.request.url}")
                val response: GenericResponse = httpResponse.body()
                Log.d(TAG, "addTransaction response: ${response.response}")
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "addTransaction error", e)
                false
            }
        } ?: false
    }

    suspend fun deleteTransaction(id: Int): Boolean {
        val baseURL = BuildConfig.BASE_URL
        return client?.let {
            try {
                val response: GenericResponse = it.delete("${baseURL}/apis/call") {
                    parameter("scope", "budget:transactions")
                    parameter("route", "transaction")
                    parameter("id", id)
                }.body()
                response.response == "ok"
            } catch (e: Exception) {
                Log.e(TAG, "deleteTransaction error", e)
                false
            }
        } ?: false
    }

    fun logout() {
        Log.d(TAG, "logout called")
        sharedPreferences.edit().clear().apply()
        _token.value = null
        _isLoggedIn.value = false
        _loggedInUsername.value = null
        _userRoles.value = emptyMap()
        _loans.value = emptyList()
        _repayments.value = emptyList()
        _cars.value = emptyList()
        _maintenanceImages.value = emptyList()
        _buckets.value = emptyList()
        _transactions.value = emptyList()
        _allTransactions.value = emptyList()
        _scheduledExpenses.value = emptyList()
        client?.close()
        client = null
    }
}
