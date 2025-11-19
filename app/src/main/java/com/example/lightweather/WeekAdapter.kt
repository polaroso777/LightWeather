package com.example.lightweather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WeekUiModel(
    val dayLabel: String,    // "Lun"
    val rangeLabel: String,  // "9–24°"
    val rainLabel: String,   // "Lluvia 30%"
    val recoText: String,    // texto de Reco.recoDia(...)
    val iconResId: Int       // R.drawable.wi_*
)

class WeekAdapter(
    private var items: List<WeekUiModel> = emptyList()
) : RecyclerView.Adapter<WeekAdapter.WeekVH>() {

    fun submitList(newItems: List<WeekUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week, parent, false)
        return WeekVH(view)
    }

    override fun onBindViewHolder(holder: WeekVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class WeekVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDay: TextView = view.findViewById(R.id.tvWeekDay)
        private val tvRange: TextView = view.findViewById(R.id.tvWeekRange)
        private val tvRain: TextView = view.findViewById(R.id.tvWeekRain)
        private val tvReco: TextView = view.findViewById(R.id.tvWeekReco)
        private val ivIcon: ImageView = view.findViewById(R.id.ivWeekIcon)

        fun bind(item: WeekUiModel) {
            tvDay.text = item.dayLabel
            tvRange.text = item.rangeLabel
            tvRain.text = item.rainLabel
            tvReco.text = item.recoText
            ivIcon.setImageResource(item.iconResId)
        }
    }
}
