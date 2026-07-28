package com.jon_is_awesome.hfg_portal_app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GenericResponse(
    val response: String? = null,
    val date: String? = null,
    val data: JsonElement? = null
)

@Serializable
data class AppRole(
    val app: String? = null,
    val role: String? = null
)

@Serializable
data class AuthResponse(
    val jwt: String? = null,
    val username: String? = null,
    val apps: List<AppRole> = emptyList()
)

@Serializable
data class CarResponse(
    val data: List<RemoteCar> = emptyList()
)

@Serializable
data class RemoteCar(
    val id: Int,
    val name: String? = null,
    val year: Int? = null,
    val make: String? = null,
    val model: String? = null,
    val license_plate: String? = null,
    val vin: String? = null,
    val mileage: String? = null
)

@Serializable
data class ServiceHistoryResponse(
    val data: List<RemoteServiceHistory> = emptyList(),
    val date: String? = null,
    val response: String? = null
)

@Serializable
data class RemoteServiceHistory(
    val id: Int,
    val item: String? = null,
    val mileage: Int? = null,
    val car_id: Int,
    val notes: String? = null,
    val service_date: String? = null,
    val next_service: JsonElement? = null
)

@Serializable
data class MaintenanceItemsResponse(
    val data: List<RemoteMaintenanceItem> = emptyList(),
    val date: String? = null,
    val response: String? = null
)

@Serializable
data class RemoteMaintenanceItem(
    val id: Int,
    val name: String? = null,
    val notes: String? = null,
    val interval_miles: Int? = null,
    val interval_months: Int? = null
)

@Serializable
data class MaintenanceImagesResponse(
    val data: List<RemoteMaintenanceImage> = emptyList(),
    val date: String? = null,
    val response: String? = null
)

@Serializable
data class RemoteMaintenanceImage(
    val id: Int,
    val name: String? = null,
    val image: String? = null,
    val image_content_type: String? = null
)

// ---- Loan Repayment ----

@Serializable
data class LoanListResponse(
    val data: List<RemoteLoan> = emptyList(),
    val date: String? = null,
    val response: String? = null
)

@Serializable
data class RemoteLoan(
    val id: Int,
    val active: Boolean? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("for") val loanFor: String? = null,
    val amount: String? = null,
    val paid_amount_tot: String? = null,
    val remaining_balance: String? = null,
    val paid_to: String? = null
)

@Serializable
data class RepaymentListResponse(
    val data: List<RemoteRepayment> = emptyList(),
    val date: String? = null,
    val response: String? = null
)

@Serializable
data class RemoteRepayment(
    val id: Int,
    val loan_id: Int? = null,
    val paid_amount: String? = null,
    val paid_amount_tot: String? = null,
    val remaining_balance: String? = null,
    val inserted_at: String? = null
)

// ---- Budget ----

@Serializable
data class BucketListResponse(
    val data: List<RemoteBucket> = emptyList(),
    val response: String? = null
)

@Serializable
data class RemoteBucket(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val balance: String? = null,
    val monthly_budget: String? = null,
    val monthly_contribution: String? = null,
    val max_balance: String? = null,
    val type: String? = null,
    val monthly_expenses: String? = null
)

@Serializable
data class TransactionListResponse(
    val data: List<RemoteTransaction> = emptyList(),
    val response: String? = null
)

@Serializable
data class RemoteTransaction(
    val id: Int,
    val amount: String? = null,
    val description: String? = null,
    val transaction_date: String? = null,
    val type: String? = null,
    val bucket_id: Int? = null,
    val scheduled_expense_id: Int? = null
)

@Serializable
data class ScheduledExpenseListResponse(
    val data: List<RemoteScheduledExpense> = emptyList(),
    val response: String? = null
)

@Serializable
data class RemoteScheduledExpense(
    val id: Int,
    val amount: String? = null,
    val description: String? = null,
    val frequency: String? = null,
    val day_of_month: Int? = null,
    val day_of_week: Int? = null,
    val month_of_year: Int? = null,
    val next_due_date: String? = null,
    val active: Boolean? = null,
    val bucket_id: Int? = null
)

@Serializable
data class AccountTotalsResponse(
    val data: RemoteAccountTotals? = null,
    val response: String? = null
)

@Serializable
data class RemoteAccountTotals(
    val month: Int? = null,
    val year: Int? = null,
    val debit_total: String? = null,
    val credit_total: String? = null,
    val sinking_total: String? = null,
    val total_money: String? = null,
    val current_month_expenses: String? = null
)

@Serializable
data class AccountHistoryResponse(
    val data: RemoteAccountHistoryData = RemoteAccountHistoryData(),
    val response: String? = null
)

@Serializable
data class RemoteAccountHistoryData(
    val from: String? = null,
    val to: String? = null,
    val interval: String? = null,
    val points: List<RemoteAccountHistoryPoint> = emptyList()
)

@Serializable
data class RemoteAccountHistoryPoint(
    val date: String? = null,
    val debit_total: String? = null,
    val credit_total: String? = null,
    val sinking_total: String? = null,
    val emergency_total: String? = null,
    val total_money: String? = null
)
