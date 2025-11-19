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
    val apparent_temperature: Double?,
    val precipitation_probability: Double?,
    val wind_speed_10m: Double?,
    val weather_code: Int?,   // para icono actual si lo quieres usar
    val is_day: Int?          // 1 = día, 0 = noche
)

data class Hourly(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val precipitation_probability: List<Double>?,
    val weather_code: List<Int>?,      // nuevo: condición por hora
    val wind_speed_10m: List<Double>?  // nuevo: viento por hora
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
            "temperature_2m,apparent_temperature,precipitation,precipitation_probability,weather_code,is_day,wind_speed_10m",
        @Query("hourly") hourly: String =
            "temperature_2m,precipitation_probability,weather_code,wind_speed_10m",
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") tz: String = "America/Mexico_City"
    ): WeatherResponse
}
