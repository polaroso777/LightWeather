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
        val prefs = requireContext().getSharedPreferences("lightweather", android.content.Context.MODE_PRIVATE)
        val lat = prefs.getString("lat", "19.4326")!!.toDouble()
        val lon = prefs.getString("lon", "-99.1332")!!.toDouble()

        val tvHeader = view.findViewById<TextView>(R.id.tvHeader)
        val rv = view.findViewById<RecyclerView>(R.id.rvHourly)
        val recoContainer = view.findViewById<LinearLayout>(R.id.recoContainer)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SimplePairAdapter(listOf("Cargando…"))

        vm.weather.observe(viewLifecycleOwner) { w ->
            if (w == null) {
                tvHeader.text = "CDMX — --° / Lluvia --%"
                rv.adapter = SimplePairAdapter(listOf("Sin datos • Revisa tu conexión"))
                return@observe
            }

            // ---------------- HEADER: Temp + Lluvia ----------------
            val temp = w.current?.temperature_2m?.toInt() ?: 0
            val prob = w.current?.precipitation_probability?.toInt() ?: 0
            val now = LocalDateTime.now(ZoneId.of("America/Mexico_City"))
            val hora = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            tvHeader.text = "CDMX — ${temp}° / Lluvia ${prob}%  •  $hora"

            // ---------------- LISTA HORARIA (24 horas) ----------------
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

            // ---------------- RECOMENDACIONES ----------------
            recoContainer.removeAllViews()

            val daily = w.daily
            val minHoy = daily?.temperature_2m_min?.firstOrNull()
            val maxHoy = daily?.temperature_2m_max?.firstOrNull()
            val probHoy = daily?.precipitation_probability_max?.firstOrNull()
            val horasLluvia = rains

            // Rango térmico
            val reco1 = Reco.rangoTermico(minHoy, maxHoy)
            // Paraguas sí/no
            val reco2 = Reco.paraguas(probHoy)
            // Ventana seca
            val reco3 = Reco.ventanaSeca(horasLluvia)

            val listaReco = listOfNotNull(reco1, reco2, reco3)

            listaReco.forEach { msg ->
                val tv = TextView(requireContext()).apply {
                    text = msg
                    textSize = 14f
                    setPadding(8, 6, 8, 6)
                }
                recoContainer.addView(tv)
            }
        }

        // Llamada inicial (una sola vez gracias a TodayVM.loaded)
        vm.load(lat, lon)
    }
}
