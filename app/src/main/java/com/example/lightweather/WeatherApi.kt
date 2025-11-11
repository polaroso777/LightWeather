package com.example.lightweather

import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(
    val latitude: Double?,
    val longitude: Double?,
    val current: Current?,
    val hourly: Hourly?,
    val daily: Daily?
)
data class Current(
    val temperature_2m: Double?,
    val precipitation_probability: Double?
)
data class Hourly(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val precipitation_probability: List<Double>?
)
data class Daily(
    val time: List<String>?,
    val temperature_2m_min: List<Double>?,
    val temperature_2m_max: List<Double>?,
    val precipitation_probability_max: List<Double>?
)

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String =
            "temperature_2m,precipitation,precipitation_probability,weather_code,is_day",
        @Query("hourly") hourly: String =
            "temperature_2m,precipitation_probability",
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") tz: String = "America/Mexico_City"
    ): WeatherResponse
}
