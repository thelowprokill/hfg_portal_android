package com.jon_is_awesome.hfg_portal_app.data.repository

import com.jon_is_awesome.hfg_portal_app.data.model.WeatherInfo
import com.jon_is_awesome.hfg_portal_app.data.remote.WeatherApiService

class WeatherRepository(private val weatherApiService: WeatherApiService) {

    suspend fun getWeatherData(lat: Double, lon: Double): WeatherInfo {
        val response = weatherApiService.getWeatherData(lat, lon)
        val current = response.current_weather
        
        return WeatherInfo(
            temperature = current.temperature,
            condition = getWeatherCondition(current.weathercode),
            iconCode = current.weathercode
        )
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear, partly cloudy, and overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow fall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}
