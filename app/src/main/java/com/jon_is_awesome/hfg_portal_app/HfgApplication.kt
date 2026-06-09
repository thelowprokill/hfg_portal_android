package com.jon_is_awesome.hfg_portal_app

import android.app.Application
import com.jon_is_awesome.hfg_portal_app.data.local.AppDatabase
import com.jon_is_awesome.hfg_portal_app.data.local.UserPreferencesRepository
import com.jon_is_awesome.hfg_portal_app.data.remote.RemoteDataSource
import com.jon_is_awesome.hfg_portal_app.data.remote.WeatherApiService
import com.jon_is_awesome.hfg_portal_app.data.repository.AppRepository
import com.jon_is_awesome.hfg_portal_app.data.repository.WeatherRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class HfgApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.appDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val remoteDataSource by lazy { RemoteDataSource(this) }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private val weatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }

    val weatherRepository by lazy {
        WeatherRepository(weatherApiService)
    }
}
