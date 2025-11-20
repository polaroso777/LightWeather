package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WeekFragment : Fragment(R.layout.fragment_week) {

    private val vm by activityViewModels<TodayVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------------------------
        // Header (mismo que Today)
        // -------------------------
        val headerView = view.findViewById<View>(R.id.includeCurrentHeaderWeek)
        val tvHeader = headerView.findViewById<TextView>(R.id.tvHeader)
        val tvTempMain = headerView.findViewById<TextView>(R.id.tvTempMain)
        val tvFeelsLike = headerView.findViewById<TextView>(R.id.tvFeelsLike)
        val tvWindSpeed = headerView.findViewById<TextView>(R.id.tvWindSpeed)

        // -------------------------
        // Summary texts
        // -------------------------
        val tvWeekSummaryRange  = view.findViewById<TextView>(R.id.tvWeekSummaryRange)
        val tvWeekSummaryRain   = view.findViewById<TextView>(R.id.tvWeekSummaryRain)
        val tvWeekSummaryAdvice = view.findViewById<TextView>(R.id.tvWeekSummaryAdvice)

        // -------------------------
        // Recycler
        // -------------------------
        val rv = view.findViewById<RecyclerView>(R.id.rvWeek)
        val weekAdapter = WeekAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = weekAdapter

        // ---------------------------------------------------------------------
        // VINCULACIÓN CON EL VM
        // ---------------------------------------------------------------------

        vm.headerText.observe(viewLifecycleOwner) { header ->
            tvHeader.text = header
        }

        vm.tempText.observe(viewLifecycleOwner) { temp ->
            tvTempMain.text = temp
        }

        vm.feelsLikeText.observe(viewLifecycleOwner) { feels ->
            tvFeelsLike.text = feels
        }

        vm.windSpeedText.observe(viewLifecycleOwner) { wind ->
            tvWindSpeed.text = wind
        }

        vm.weekSummaryRangeText.observe(viewLifecycleOwner) { txt ->
            tvWeekSummaryRange.text = txt
        }

        vm.weekSummaryRainText.observe(viewLifecycleOwner) { txt ->
            tvWeekSummaryRain.text = txt
        }

        vm.weekSummaryAdviceText.observe(viewLifecycleOwner) { txt ->
            tvWeekSummaryAdvice.text = txt
        }

        vm.weekItems.observe(viewLifecycleOwner) { items ->
            weekAdapter.submitList(items)
        }
    }
}
