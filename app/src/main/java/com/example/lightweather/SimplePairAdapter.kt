package com.example.lightweather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimplePairAdapter(private val items: List<String>) :
    RecyclerView.Adapter<SimplePairAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t1: TextView = v.findViewById(android.R.id.text1)
        val t2: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int): VH {
        val v = LayoutInflater.from(p.context)
            .inflate(android.R.layout.simple_list_item_2, p, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val parts = items[i].split("•")
        h.t1.text = parts.getOrNull(0)?.trim().orEmpty()
        h.t2.text = parts.getOrNull(1)?.trim().orEmpty()
    }
}
