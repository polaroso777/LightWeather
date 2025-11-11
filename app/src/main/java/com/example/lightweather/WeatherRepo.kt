package com.example.lightweather

class WeatherRepo {
    suspend fun getForecast(lat: Double, lon: Double) =
        ApiClient.service.getForecast(lat, lon)
}
