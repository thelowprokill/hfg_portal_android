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
data class AuthResponse(
    val jwt: String? = null,
    val username: String? = null
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
