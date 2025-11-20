package com.example.lightweather
data class WeekUiModel(
    val dayLabel: String,   // "Lun", "Mar", "Mié"
    val rangeLabel: String, // "18–25°"
    val rainLabel: String,  // "Lluvia 40%"
    val recoText: String,   // Recomendación diaria
    val iconResId: Int      // R.drawable.ic_clima
)
