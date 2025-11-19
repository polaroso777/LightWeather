package com.example.lightweather

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayVM : ViewModel() {

    private val repo = WeatherRepo()

    private val _weather = MutableLiveData<WeatherResponse?>()
    val weather: LiveData<WeatherResponse?> = _weather

    // LiveData derivados para la UI
    private val _temp = MutableLiveData<Double?>()
    val temp: LiveData<Double?> = _temp

    private val _feelsLike = MutableLiveData<Double?>()
    val feelsLike: LiveData<Double?> = _feelsLike

    private val _windSpeed = MutableLiveData<Double?>()
    val windSpeed: LiveData<Double?> = _windSpeed

    fun load(lat: Double = 19.4326, lon: Double = -99.1332) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = repo.getForecast(lat, lon)

                _weather.postValue(resp)
                _temp.postValue(resp.current?.temperature_2m)
                _feelsLike.postValue(resp.current?.apparent_temperature)
                _windSpeed.postValue(resp.current?.wind_speed_10m)

            } catch (e: Exception) {
                e.printStackTrace()
                _weather.postValue(null)
                _temp.postValue(null)
                _feelsLike.postValue(null)
                _windSpeed.postValue(null)
            }
        }
    }
}
