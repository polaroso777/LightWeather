package com.example.lightweather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WeatherMapper {

    private val ZONE_ID: ZoneId = ZoneId.of("America/Mexico_City")
    private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // -------------------------------------------------------------------------
    // HEADER
    // -------------------------------------------------------------------------
    fun buildHeader(
        current: Current?,
        placeName: String,
        now: LocalDateTime = LocalDateTime.now(ZONE_ID)
    ): String {
        val hora = now.format(HOUR_FORMAT)
        val tempInt = current?.temperature_2m?.toInt()
        val probInt = current?.precipitation_probability?.toInt()

        return if (tempInt != null && probInt != null) {
            "$placeName — ${tempInt}° / Lluvia ${probInt}%  •  $hora"
        } else {
            "$placeName — sin datos actuales • $hora"
        }
    }

    // -------------------------------------------------------------------------
    // HOY: LISTA HORARIA
    // -------------------------------------------------------------------------
    fun mapTodayHourly(weather: WeatherResponse): List<HourlyUiModel> {
        val hourly = weather.hourly ?: return emptyList()
        val times = hourly.time ?: return emptyList()

        val temps = hourly.temperature_2m ?: emptyList()
        val rains = hourly.precipitation_probability ?: emptyList()
        val codes = hourly.weather_code ?: emptyList()
        val winds = hourly.wind_speed_10m ?: emptyList()

        val now = LocalDateTime.now(ZONE_ID)
        val todayDate = now.toLocalDate()

        val indicesDeHoyDesdeAhora = times.mapIndexedNotNull { index, tStr ->
            val dt = runCatching { LocalDateTime.parse(tStr) }.getOrNull()
                ?: return@mapIndexedNotNull null

            if (dt.toLocalDate() != todayDate) return@mapIndexedNotNull null
            if (dt.hour >= now.hour) index else null
        }

        val indicesFinales = if (indicesDeHoyDesdeAhora.isNotEmpty()) {
            indicesDeHoyDesdeAhora
        } else {
            times.mapIndexedNotNull { index, tStr ->
                val dt = runCatching { LocalDateTime.parse(tStr) }.getOrNull()
                    ?: return@mapIndexedNotNull null

                if (dt.toLocalDate() == todayDate) index else null
            }
        }

        return indicesFinales.map { i ->
            val dt = runCatching { LocalDateTime.parse(times[i]) }.getOrNull()
            val hourLabel = dt?.format(HOUR_FORMAT) ?: times[i].substringAfter("T").take(5)

            val tVal = temps.getOrNull(i)
            val tInt = tVal?.toInt() ?: 0
            val pr = rains.getOrNull(i)?.toInt() ?: 0
            val code = codes.getOrNull(i)
            val wind = winds.getOrNull(i)

            val iconRes = IconMapper.iconForHour(
                weatherCode = code,
                windSpeed = wind,
                temp = tVal
            )

            HourlyUiModel(
                hour = hourLabel,
                label = "${tInt}° / Lluvia ${pr}%",
                iconResId = iconRes
            )
        }
    }

    // -------------------------------------------------------------------------
    // SEMANA: SOLO LA LISTA (sin resúmenes)
    // -------------------------------------------------------------------------
    data class WeekUiResult(
        val items: List<WeekUiModel>
    )

    fun buildWeekUi(daily: Daily?): WeekUiResult {
        if (daily == null || daily.time.isNullOrEmpty()) {
            return WeekUiResult(items = emptyList())
        }

        val times = daily.time
        val mins = daily.temperature_2m_min ?: emptyList()
        val maxs = daily.temperature_2m_max ?: emptyList()
        val rains = daily.precipitation_probability_max ?: emptyList()
        val codes = daily.weather_code ?: emptyList()

        val items = times.indices.take(7).map { i ->
            val tmin = mins.getOrNull(i)
            val tmax = maxs.getOrNull(i)
            val pr = rains.getOrNull(i)
            val code = codes.getOrNull(i)

            val tminInt = tmin?.toInt() ?: 0
            val tmaxInt = tmax?.toInt() ?: 0
            val prInt = pr?.toInt() ?: 0

            val dayText = dayLabel(times[i])
            val rangeText = "${tminInt}–${tmaxInt}°"
            val rainText = "Lluvia ${prInt}%"

            val avgTemp: Double? = if (tmin != null && tmax != null) {
                (tmin + tmax) / 2.0
            } else null

            val iconRes = IconMapper.iconForHour(
                weatherCode = code,
                windSpeed = null,
                temp = avgTemp
            )

            val reco = Reco.recoDia(tmin, tmax, pr)
            val recoText = if (reco != null) {
                listOf(
                    when (reco.rango) {
                        Reco.RangoTipo.AMPLIO -> "rango amplio"
                        Reco.RangoTipo.ESTABLE -> "rango estable"
                    },
                    when (reco.lluvia) {
                        Reco.LluviaTipo.ALTA -> "lluvia alta, lleva paraguas"
                        Reco.LluviaTipo.MEDIA -> "lluvia moderada"
                        Reco.LluviaTipo.BAJA -> "lluvia baja"
                    },
                    when (reco.clima) {
                        Reco.ClimaTipo.FRIO -> "día frío"
                        Reco.ClimaTipo.FRESCO -> "día fresco"
                        Reco.ClimaTipo.AGRADABLE -> "clima agradable"
                        Reco.ClimaTipo.CALUROSO -> "día caluroso"
                    }
                ).joinToString(" · ")
            } else {
                "Sin datos diarios"
            }

            WeekUiModel(
                dayLabel = dayText,
                rangeLabel = rangeText,
                rainLabel = rainText,
                recoText = recoText,
                iconResId = iconRes
            )
        }

        return WeekUiResult(items)
    }

    // -------------------------------------------------------------------------
    // UTIL
    // -------------------------------------------------------------------------
    private fun dayLabel(isoDate: String): String {
        val date = LocalDate.parse(isoDate)
        return when (date.dayOfWeek.value) {
            1 -> "Lun"
            2 -> "Mar"
            3 -> "Mié"
            4 -> "Jue"
            5 -> "Vie"
            6 -> "Sáb"
            else -> "Dom"
        }
    }
}
