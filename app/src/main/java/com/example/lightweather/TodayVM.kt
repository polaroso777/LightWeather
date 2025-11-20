package com.example.lightweather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayVM : ViewModel() {

    // -------------------------------------------------------------------------
    // WEATHER RAW
    // -------------------------------------------------------------------------
    private val _weather = MutableLiveData<WeatherResponse?>()
    val weather: LiveData<WeatherResponse?> = _weather

    // -------------------------------------------------------------------------
    // TODAY VALUES (CRUDO + FORMATEADO)
    // -------------------------------------------------------------------------
    private val _temp = MutableLiveData<Double?>()
    val temp: LiveData<Double?> = _temp

    private val _feelsLike = MutableLiveData<Double?>()
    val feelsLike: LiveData<Double?> = _feelsLike

    private val _windSpeed = MutableLiveData<Double?>()
    val windSpeed: LiveData<Double?> = _windSpeed

    private val _tempText = MutableLiveData<String>()
    val tempText: LiveData<String> = _tempText

    private val _feelsLikeText = MutableLiveData<String>()
    val feelsLikeText: LiveData<String> = _feelsLikeText

    private val _windSpeedText = MutableLiveData<String>()
    val windSpeedText: LiveData<String> = _windSpeedText

    private val _headerText = MutableLiveData<String>()
    val headerText: LiveData<String> = _headerText

    // -------------------------------------------------------------------------
    // TODAY RECOMMENDATIONS
    // -------------------------------------------------------------------------
    private val _todayRecommendations = MutableLiveData<List<String>?>()
    val todayRecommendations: LiveData<List<String>?> = _todayRecommendations

    // -------------------------------------------------------------------------
    // HOURLY LIST
    // -------------------------------------------------------------------------
    private val _hourlyItems = MutableLiveData<List<HourlyUiModel>>()
    val hourlyItems: LiveData<List<HourlyUiModel>> = _hourlyItems

    // -------------------------------------------------------------------------
    // WEEKLY LIST + SUMMARIES
    // -------------------------------------------------------------------------
    private val _weekItems = MutableLiveData<List<WeekUiModel>>()
    val weekItems: LiveData<List<WeekUiModel>> = _weekItems

    private val _weekSummaryRangeText = MutableLiveData<String>()
    val weekSummaryRangeText: LiveData<String> = _weekSummaryRangeText

    private val _weekSummaryRainText = MutableLiveData<String>()
    val weekSummaryRainText: LiveData<String> = _weekSummaryRainText

    private val _weekSummaryAdviceText = MutableLiveData<String>()
    val weekSummaryAdviceText: LiveData<String> = _weekSummaryAdviceText



    // -------------------------------------------------------------------------
    // FORMATTERS (OBJETO → TEXTO)
    // -------------------------------------------------------------------------

    private fun formatRangoSemana(r: Reco.RangoTermico?): String {
        return if (r == null) {
            "En la semana: sin datos"
        } else {
            "En la semana: ${r.min}–${r.max}°"
        }
    }

    private fun formatLluviaSemana(t: Reco.LluviaTipoSemana?): String {
        return when (t) {
            null -> "Resumen de lluvia no disponible"
            Reco.LluviaTipoSemana.MUY_LLUVIOSA ->
                "Semana muy lluviosa, varios días con lluvia alta"
            Reco.LluviaTipoSemana.LLUVIA_ALTA ->
                "Habrá algunos días con lluvia alta, revisa el pronóstico diario"
            Reco.LluviaTipoSemana.LLUVIA_VARIADA ->
                "Lluvias moderadas en varios días de la semana"
            Reco.LluviaTipoSemana.MAYORMENTE_SECA ->
                "Semana mayormente seca, lluvias poco probables"
            Reco.LluviaTipoSemana.AISLADA ->
                "Lluvia aislada en algunos días de la semana"
        }
    }

    private fun formatConsejoSemana(c: Reco.ConsejoSemana?): String {
        return when (c) {
            null -> "Sin consejo general para esta semana"
            Reco.ConsejoSemana.FRIA ->
                "Semana fría: privilegia ropa abrigadora y varias capas."
            Reco.ConsejoSemana.FRESCA ->
                "Semana fresca: usa capas ligeras y suéteres medianos."
            Reco.ConsejoSemana.TEMPLADA ->
                "Semana templada: ropa ligera, una capa extra en mañanas y noches."
            Reco.ConsejoSemana.CALUROSA ->
                "Semana calurosa: ropa muy ligera y buena hidratación."
        }
    }


    // -------------------------------------------------------------------------
    // LOAD LOGIC
    // -------------------------------------------------------------------------
    fun load(
        lat: Double = 19.4326,
        lon: Double = -99.1332,
        placeName: String = "Ubicación actual"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.service.getForecast(lat, lon)
                val current = resp.current

                _weather.postValue(resp)

                // HOY
                val tempNow = current?.temperature_2m
                val feelsNow = current?.apparent_temperature
                val windNow = current?.wind_speed_10m

                _temp.postValue(tempNow)
                _feelsLike.postValue(feelsNow)
                _windSpeed.postValue(windNow)

                _tempText.postValue(tempNow?.let { "${it.toInt()}°C" } ?: "--°C")
                _feelsLikeText.postValue(
                    feelsNow?.let { "Sensación térmica: ${it.toInt()}°C" }
                        ?: "Sensación térmica: --°C"
                )
                _windSpeedText.postValue(
                    windNow?.let { "Viento: ${it.toInt()} km/h" }
                        ?: "Viento: -- km/h"
                )

                _headerText.postValue(
                    WeatherMapper.buildHeader(current, placeName)
                )

                // RECO HOY
                val daily = resp.daily
                val minHoy = daily?.temperature_2m_min?.firstOrNull()
                val maxHoy = daily?.temperature_2m_max?.firstOrNull()
                val probHoy = daily?.precipitation_probability_max?.firstOrNull()
                val horasLluvia = resp.hourly?.precipitation_probability

                // Convertir objetos → Strings finales
                val recoList = mutableListOf<String>()

                Reco.rangoTermico(minHoy, maxHoy)?.let {
                    recoList.add("Rango térmico: ${it.min}–${it.max}°")
                }

                Reco.paraguas(probHoy)?.let {
                    val msg = when (it.recomendacion) {
                        Reco.TipoParaguas.ALTO -> "Recomendación: lleva paraguas"
                        Reco.TipoParaguas.MEDIO -> "Posibles lluvias"
                        Reco.TipoParaguas.BAJO -> "Baja probabilidad de lluvia"
                    }
                    recoList.add(msg)
                }

                Reco.ventanaSeca(horasLluvia)?.let {
                    recoList.add("Ventana seca: ${it.startHour}:00–${it.endHour}:00")
                }

                Reco.sensacionTermicaActual(tempNow, feelsNow)?.let {
                    val msg = when (it.tipo) {
                        Reco.TipoSensacion.MAS_CALOR ->
                            "Se siente más caluroso de lo que marca el termómetro"
                        Reco.TipoSensacion.MAS_FRIO ->
                            "Se siente más frío de lo que marca el termómetro"
                        Reco.TipoSensacion.SIMILAR ->
                            "La sensación térmica es similar a la temperatura"
                    }
                    recoList.add(msg)
                }

                Reco.vientoHoy(windNow)?.let {
                    val msg = when (it.tipo) {
                        Reco.TipoViento.FUERTE ->
                            "Viento fuerte, considera chamarra o rompevientos"
                        Reco.TipoViento.MODERADO ->
                            "Viento moderado, podría sentirse más fresco"
                        Reco.TipoViento.LIGERO ->
                            "Viento ligero, condiciones agradables"
                        Reco.TipoViento.CALMO ->
                            "Apenas hay viento, ambiente tranquilo"
                    }
                    recoList.add(msg)
                }

                _todayRecommendations.postValue(
                    if (recoList.isNotEmpty()) recoList else null
                )

                // LISTA HORARIA
                _hourlyItems.postValue(
                    WeatherMapper.mapTodayHourly(resp)
                )

                // SEMANA
                val mins = daily?.temperature_2m_min
                val maxs = daily?.temperature_2m_max
                val rains = daily?.precipitation_probability_max

                val summary = Reco.resumenSemana(mins, maxs, rains)

                _weekSummaryRangeText.postValue(formatRangoSemana(summary.rango))
                _weekSummaryRainText.postValue(formatLluviaSemana(summary.lluvia))
                _weekSummaryAdviceText.postValue(formatConsejoSemana(summary.consejo))

                // LISTA SEMANAL
                val weekResult = WeatherMapper.buildWeekUi(resp.daily)

                if (weekResult.items.isEmpty()) {
                    _weekItems.postValue(
                        listOf(
                            WeekUiModel(
                                dayLabel = "—",
                                rangeLabel = "Sin datos",
                                rainLabel = "",
                                recoText = "Sin datos diarios",
                                iconResId = R.drawable.wi_thermometer_exterior
                            )
                        )
                    )
                } else {
                    _weekItems.postValue(weekResult.items)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _weather.postValue(null)

                _tempText.postValue("--°C")
                _feelsLikeText.postValue("Sensación térmica: --°C")
                _windSpeedText.postValue("Viento: -- km/h")
                _headerText.postValue("$placeName — sin datos")

                _todayRecommendations.postValue(null)
                _hourlyItems.postValue(emptyList())

                _weekItems.postValue(
                    listOf(
                        WeekUiModel(
                            dayLabel = "—",
                            rangeLabel = "Sin datos",
                            rainLabel = "",
                            recoText = "Sin datos diarios",
                            iconResId = R.drawable.wi_thermometer_exterior
                        )
                    )
                )

                _weekSummaryRangeText.postValue("En la semana: sin datos")
                _weekSummaryRainText.postValue("Resumen de lluvia no disponible")
                _weekSummaryAdviceText.postValue("Sin consejo general para esta semana")
            }
        }
    }
}
