package com.jon_is_awesome.hfg_portal_app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_logs")
data class CarLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val date: Long,
    val cost: Double,
    val notes: String,
    val mileage: Int,
    val reminderDate: Long? = null
)
