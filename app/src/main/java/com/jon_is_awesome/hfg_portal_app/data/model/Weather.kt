package com.jon_is_awesome.hfg_portal_app.data.model

data class WeatherResponse(
    val current_weather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)

data class WeatherInfo(
    val temperature: Double,
    val condition: String,
    val iconCode: Int
)
