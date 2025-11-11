package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodayFragment : Fragment(R.layout.fragment_today) {

    private val vm by activityViewModels<TodayVM>()   // 👈 compartido con Semana

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)
        val rv = view.findViewById<RecyclerView>(R.id.rvHourly)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SimplePairAdapter(listOf("Cargando…"))

        vm.weather.observe(viewLifecycleOwner) { w ->
            if (w == null) {
                tvHeader.text = "CDMX — --° / Lluvia --%"
                rv.adapter = SimplePairAdapter(listOf("Sin datos • Revisa tu conexión"))
                return@observe
            }
            val temp = w.current?.temperature_2m?.toInt() ?: 0
            val prob = w.current?.precipitation_probability?.toInt() ?: 0
            val now = LocalDateTime.now(ZoneId.of("America/Mexico_City"))
            val hora = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            tvHeader.text = "CDMX — ${temp}° / Lluvia ${prob}%  •  $hora"

            val times = w.hourly?.time.orEmpty()
            val temps = w.hourly?.temperature_2m.orEmpty()
            val rains = w.hourly?.precipitation_probability.orEmpty()

            val today = now.toLocalDate().toString()
            val currentHour = String.format("%02d:00", now.hour)
            val firstIdx = times.indexOfFirst { it.startsWith(today) && it.endsWith(currentHour) }
                .let { if (it >= 0) it else times.indexOfFirst { it.startsWith(today) } }
                .coerceAtLeast(0)
            val endIdx = (firstIdx + 24).coerceAtMost(times.size)

            val items = (firstIdx until endIdx).map { i ->
                val hh = times[i].substringAfter('T')
                val t = temps.getOrNull(i)?.toInt() ?: 0
                val pr = rains.getOrNull(i)?.toInt() ?: 0
                "$hh • $t° / Lluvia $pr%"
            }
            rv.adapter = SimplePairAdapter(items)
        }

        // dispara la carga (y Semana observará el mismo VM)
        vm.load()
    }
}
