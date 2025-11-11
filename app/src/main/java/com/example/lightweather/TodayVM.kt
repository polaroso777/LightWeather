package com.example.lightweather

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayVM : ViewModel() {
    private val repo = WeatherRepo()

    private val _weather = MutableLiveData<WeatherResponse?>()
    val weather: LiveData<WeatherResponse?> = _weather

    fun load(lat: Double = 19.4326, lon: Double = -99.1332) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _weather.postValue(repo.getForecast(lat, lon))
            } catch (e: Exception) {
                e.printStackTrace()
                _weather.postValue(null)
            }
        }
    }
}
