package com.example.lightweather

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class WeekFragment : Fragment(R.layout.fragment_week) {

    private val vm by activityViewModels<TodayVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvWeek)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SimplePairAdapter(listOf("Cargando…"))

        vm.weather.observe(viewLifecycleOwner) { w ->
            val d = w?.daily
            if (d == null || d.time.isNullOrEmpty()) {
                rv.adapter = SimplePairAdapter(listOf("Sin datos diarios"))
                return@observe
            }

            val times = d.time
            val mins = d.temperature_2m_min ?: emptyList()
            val maxs = d.temperature_2m_max ?: emptyList()
            val rains = d.precipitation_probability_max ?: emptyList()

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

                val tminInt = tmin?.toInt() ?: 0
                val tmaxInt = tmax?.toInt() ?: 0
                val prInt   = pr?.toInt() ?: 0

                val linea1 = "${dayLabel(times[i])}  ${tminInt}–${tmaxInt}°"
                val descr = "Lluvia ${prInt}% · ${Reco.recoDia(tmin, tmax, pr)}"
                "$linea1 • $descr"
            }

            rv.adapter = SimplePairAdapter(items)
        }
    }
}
