package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodayFragment : Fragment(R.layout.fragment_today) {

    private val vm by activityViewModels<TodayVM>()   // mismo VM que Semana

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext()
            .getSharedPreferences("lightweather", android.content.Context.MODE_PRIVATE)
        val lat = prefs.getString("lat", "19.4326")!!.toDouble()
        val lon = prefs.getString("lon", "-99.1332")!!.toDouble()

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)
        val rv = view.findViewById<RecyclerView>(R.id.rvHourly)
        val recoContainer = view.findViewById<LinearLayout>(R.id.recoContainer)

        // Nuevos views para clima actual
        val tvTempMain = view.findViewById<TextView>(R.id.tvTempMain)
        val tvFeelsLike = view.findViewById<TextView>(R.id.tvFeelsLike)
        val tvWindSpeed = view.findViewById<TextView>(R.id.tvWindSpeed)

        // Adapter horario con iconos dinámicos
        val hourlyAdapter = HourlyAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = hourlyAdapter

        // --------- Observers de campos derivados del VM ---------
        vm.temp.observe(viewLifecycleOwner) { t ->
            tvTempMain.text = if (t != null) "${t.toInt()}°C" else "--°C"
        }

        vm.feelsLike.observe(viewLifecycleOwner) { f ->
            tvFeelsLike.text = if (f != null)
                "Sensación térmica: ${f.toInt()}°C"
            else
                "Sensación térmica: --°C"
        }

        vm.windSpeed.observe(viewLifecycleOwner) { wSpeed ->
            tvWindSpeed.text = if (wSpeed != null)
                "Viento: ${wSpeed.toInt()} km/h"
            else
                "Viento: -- km/h"
        }

        // --------- Observer principal del WeatherResponse ---------
        vm.weather.observe(viewLifecycleOwner) { w ->
            if (w == null) {
                tvHeader.text = "CDMX — --° / Lluvia --%"
                hourlyAdapter.submitList(emptyList())

                // reset visual de los nuevos campos
                tvTempMain.text = "--°C"
                tvFeelsLike.text = "Sensación térmica: --°C"
                tvWindSpeed.text = "Viento: -- km/h"
                return@observe
            }

            // ---------------- HEADER: Temp + Lluvia ----------------
            val temp = w.current?.temperature_2m?.toInt() ?: 0
            val prob = w.current?.precipitation_probability?.toInt() ?: 0
            val now = LocalDateTime.now(ZoneId.of("America/Mexico_City"))
            val hora = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            tvHeader.text = "CDMX — ${temp}° / Lluvia ${prob}%  •  $hora"

            // ---------------- LISTA HORARIA (solo resto del día de hoy) ----------------
            val times = w.hourly?.time.orEmpty()
            val temps = w.hourly?.temperature_2m.orEmpty()
            val rains = w.hourly?.precipitation_probability.orEmpty()
            val codes = w.hourly?.weather_code.orEmpty()
            val winds = w.hourly?.wind_speed_10m.orEmpty()

            val todayDate = now.toLocalDate().toString()   // "2025-11-18"
            val currentHour = now.hour                     // 0–23

            // Índices de horas de HOY desde la hora actual
            val indicesDeHoyDesdeAhora = times.mapIndexedNotNull { index, tStr ->
                if (!tStr.startsWith(todayDate)) return@mapIndexedNotNull null
                val hourStr = tStr.substringAfter('T').substring(0, 2)
                val hour = hourStr.toIntOrNull() ?: return@mapIndexedNotNull null
                if (hour >= currentHour) index else null
            }

            // Si por alguna razón no hay horas >= ahora, usamos todas las de hoy
            val indicesFinales = if (indicesDeHoyDesdeAhora.isNotEmpty()) {
                indicesDeHoyDesdeAhora
            } else {
                times.mapIndexedNotNull { index, tStr ->
                    if (tStr.startsWith(todayDate)) index else null
                }
            }

            val hourlyItems = indicesFinales.map { i ->
                val fullTime = times[i].substringAfter('T')   // "18:00"
                val hourLabel = fullTime.substring(0, 5)      // "18:00"

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

            hourlyAdapter.submitList(hourlyItems)

            // ---------------- RECOMENDACIONES ----------------
            recoContainer.removeAllViews()

            val daily = w.daily
            val minHoy = daily?.temperature_2m_min?.firstOrNull()
            val maxHoy = daily?.temperature_2m_max?.firstOrNull()
            val probHoy = daily?.precipitation_probability_max?.firstOrNull()
            val horasLluvia = rains

            val tempNow = w.current?.temperature_2m
            val feelsNow = w.current?.apparent_temperature
            val windNow = w.current?.wind_speed_10m

            val reco1 = Reco.rangoTermico(minHoy, maxHoy)
            val reco2 = Reco.paraguas(probHoy)
            val reco3 = Reco.ventanaSeca(horasLluvia)
            val reco4 = Reco.sensacionTermicaActual(tempNow, feelsNow)
            val reco5 = Reco.vientoHoy(windNow)

            val listaReco = listOfNotNull(reco1, reco2, reco3, reco4, reco5)

            listaReco.forEach { msg ->
                val tv = TextView(requireContext()).apply {
                    text = msg
                    textSize = 14f
                    setPadding(8, 6, 8, 6)
                }
                recoContainer.addView(tv)
            }
        }

        // Llamada inicial
        vm.load(lat, lon)
    }
}
