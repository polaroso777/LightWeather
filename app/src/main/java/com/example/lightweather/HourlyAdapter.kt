package com.example.lightweather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HourlyUiModel(
    val hour: String,      // "18:00"
    val label: String,     // "22° / Lluvia 5%"
    val iconResId: Int     // R.drawable.wi_*
)

class HourlyAdapter(
    private var items: List<HourlyUiModel> = emptyList()
) : RecyclerView.Adapter<HourlyAdapter.HourlyVH>() {

    fun submitList(newItems: List<HourlyUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly, parent, false)
        return HourlyVH(view)
    }

    override fun onBindViewHolder(holder: HourlyVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HourlyVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHour: TextView = view.findViewById(R.id.tvHour)
        private val tvTempRain: TextView = view.findViewById(R.id.tvTempRain)
        private val ivIcon: ImageView = view.findViewById(R.id.ivWeatherIcon)

        fun bind(item: HourlyUiModel) {
            tvHour.text = item.hour
            tvTempRain.text = item.label
            ivIcon.setImageResource(item.iconResId)
        }
    }
}
