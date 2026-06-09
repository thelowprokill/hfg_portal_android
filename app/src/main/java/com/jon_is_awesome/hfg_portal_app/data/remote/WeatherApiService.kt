package com.jon_is_awesome.hfg_portal_app.data.remote

import com.jon_is_awesome.hfg_portal_app.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("windspeed_unit") windUnit: String = "mph"
    ): WeatherResponse
}
