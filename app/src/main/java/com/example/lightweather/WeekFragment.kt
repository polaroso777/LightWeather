package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WeekFragment : Fragment(R.layout.fragment_week) {

    private val vm by activityViewModels<TodayVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvWeek)

        // --- Encabezado reutilizable (include) ---
        val headerView = view.findViewById<View>(R.id.includeCurrentHeaderWeek)
        val tvHeader = headerView.findViewById<TextView>(R.id.tvHeader)
        val tvTempMain = headerView.findViewById<TextView>(R.id.tvTempMain)
        val tvFeelsLike = headerView.findViewById<TextView>(R.id.tvFeelsLike)
        val tvWindSpeed = headerView.findViewById<TextView>(R.id.tvWindSpeed)

        // TextViews para el resumen semanal global
        val tvWeekSummaryRange  = view.findViewById<TextView>(R.id.tvWeekSummaryRange)
        val tvWeekSummaryRain   = view.findViewById<TextView>(R.id.tvWeekSummaryRain)
        val tvWeekSummaryAdvice = view.findViewById<TextView>(R.id.tvWeekSummaryAdvice)

        val weekAdapter = WeekAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = weekAdapter

        // Nombre del lugar guardado por MainActivity
        val prefs = requireContext()
            .getSharedPreferences("lightweather", android.content.Context.MODE_PRIVATE)
        val placeName = prefs.getString("place_name", "Ubicación actual")

        // --------- Observers de campos derivados del VM (igual que Today) ---------
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

        vm.weather.observe(viewLifecycleOwner) { w ->
            // --------- Encabezado (compartido con Today) ---------
            if (w == null) {
                tvHeader.text = "$placeName — --° / Lluvia --%"

                tvWeekSummaryRange.text  = "En la semana: sin datos"
                tvWeekSummaryRain.text   = "Resumen de lluvia no disponible"
                tvWeekSummaryAdvice.text = "Sin consejo general para esta semana"

                val emptyItem = WeekUiModel(
                    dayLabel = "—",
                    rangeLabel = "Sin datos",
                    rainLabel = "",
                    recoText = "Sin datos diarios",
                    iconResId = R.drawable.wi_thermometer_exterior
                )
                weekAdapter.submitList(listOf(emptyItem))
                return@observe
            } else {
                val temp = w.current?.temperature_2m?.toInt() ?: 0
                val prob = w.current?.precipitation_probability?.toInt() ?: 0
                val now = LocalDateTime.now(ZoneId.of("America/Mexico_City"))
                val hora = now.format(DateTimeFormatter.ofPattern("HH:mm"))

                tvHeader.text = "$placeName — ${temp}° / Lluvia ${prob}%  •  $hora"
            }

            // --------- Lista semanal ---------
            val d = w.daily
            if (d == null || d.time.isNullOrEmpty()) {
                tvWeekSummaryRange.text  = "En la semana: sin datos"
                tvWeekSummaryRain.text   = "Resumen de lluvia no disponible"
                tvWeekSummaryAdvice.text = "Sin consejo general para esta semana"

                val emptyItem = WeekUiModel(
                    dayLabel = "—",
                    rangeLabel = "Sin datos",
                    rainLabel = "",
                    recoText = "Sin datos diarios",
                    iconResId = R.drawable.wi_thermometer_exterior
                )
                weekAdapter.submitList(listOf(emptyItem))
                return@observe
            }

            val times = d.time
            val mins = d.temperature_2m_min ?: emptyList()
            val maxs = d.temperature_2m_max ?: emptyList()
            val rains = d.precipitation_probability_max ?: emptyList()
            val codes = d.weather_code ?: emptyList()

            fun dayLabel(isoDate: String): String {
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

            val items = times.indices.take(7).map { i ->
                val tmin = mins.getOrNull(i)
                val tmax = maxs.getOrNull(i)
                val pr   = rains.getOrNull(i)
                val code = codes.getOrNull(i)

                val tminInt = tmin?.toInt() ?: 0
                val tmaxInt = tmax?.toInt() ?: 0
                val prInt   = pr?.toInt() ?: 0

                val dayText = dayLabel(times[i])
                val rangeText = "${tminInt}–${tmaxInt}°"
                val rainText = "Lluvia ${prInt}%"

                // Temperatura promedio del día para el icono
                val avgTemp: Double? = if (tmin != null && tmax != null) {
                    (tmin + tmax) / 2.0
                } else {
                    null
                }

                val iconRes = IconMapper.iconForHour(
                    weatherCode = code,
                    windSpeed = null,   // no tenemos viento diario
                    temp = avgTemp
                )

                val recoText = Reco.recoDia(tmin, tmax, pr)

                WeekUiModel(
                    dayLabel = dayText,
                    rangeLabel = rangeText,
                    rainLabel = rainText,
                    recoText = recoText,
                    iconResId = iconRes
                )
            }

            weekAdapter.submitList(items)

            // --------- Resumen SEMANAL global ---------
            val resumenRangoSemana   = Reco.rangoTermicoSemana(mins, maxs)
            val resumenLluviaSemana  = Reco.lluviaSemana(rains)
            val resumenConsejoSemana = Reco.consejoGeneralSemana(mins, maxs)

            tvWeekSummaryRange.text =
                resumenRangoSemana ?: "En la semana: sin datos"

            tvWeekSummaryRain.text =
                resumenLluviaSemana ?: "Resumen de lluvia no disponible"

            tvWeekSummaryAdvice.text =
                resumenConsejoSemana ?: "Sin consejo general para esta semana"
        }
    }
}
