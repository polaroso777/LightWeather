package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TodayFragment : Fragment(R.layout.fragment_today) {

    private val vm by activityViewModels<TodayVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext()
            .getSharedPreferences("lightweather", android.content.Context.MODE_PRIVATE)
        val lat = prefs.getString("lat", "19.4326")!!.toDouble()
        val lon = prefs.getString("lon", "-99.1332")!!.toDouble()
        val placeName = prefs.getString("place_name", "Ubicación actual") ?: "Ubicación actual"

        // ----- Referencias del header -----
        val headerView = view.findViewById<View>(R.id.includeCurrentHeaderToday)
        val tvHeader = headerView.findViewById<TextView>(R.id.tvHeader)
        val tvTempMain = headerView.findViewById<TextView>(R.id.tvTempMain)
        val tvFeelsLike = headerView.findViewById<TextView>(R.id.tvFeelsLike)
        val tvWindSpeed = headerView.findViewById<TextView>(R.id.tvWindSpeed)

        // ----- Lista por hora -----
        val rv = view.findViewById<RecyclerView>(R.id.rvHourly)
        val hourlyAdapter = HourlyAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = hourlyAdapter

        // ----- Contenedor de recomendaciones -----
        val recoContainer = view.findViewById<LinearLayout>(R.id.recoContainer)

        // ----- Observers del header -----
        vm.tempText.observe(viewLifecycleOwner) { text ->
            tvTempMain.text = text
        }

        vm.feelsLikeText.observe(viewLifecycleOwner) { text ->
            tvFeelsLike.text = text
        }

        vm.windSpeedText.observe(viewLifecycleOwner) { text ->
            tvWindSpeed.text = text
        }

        vm.headerText.observe(viewLifecycleOwner) { header ->
            tvHeader.text = header ?: "$placeName — sin datos"
        }

        // ----- Observers de recomendaciones -----
        vm.todayRecommendations.observe(viewLifecycleOwner) { listaReco ->
            recoContainer.removeAllViews()

            if (listaReco.isNullOrEmpty()) return@observe

            listaReco.forEach { msg ->
                val tv = TextView(requireContext()).apply {
                    text = msg
                    textSize = 14f
                    setPadding(8, 6, 8, 6)

                    // 🔹 Color dinámico compatible con modo claro/oscuro
                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.lw_blue_light
                        )
                    )
                }
                recoContainer.addView(tv)
            }
        }

        // ----- Lista horaria -----
        vm.hourlyItems.observe(viewLifecycleOwner) { items ->
            hourlyAdapter.submitList(items ?: emptyList())
        }

        // ----- Carga inicial -----
        vm.load(lat, lon, placeName)
    }
}
