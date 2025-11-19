package com.example.lightweather

object Reco {

    // -------- HOY --------

    fun rangoTermico(min: Double?, max: Double?): String? {
        if (min == null || max == null) return null
        return "Rango térmico: ${min.toInt()}–${max.toInt()}°"
    }

    fun paraguas(prob: Double?): String? {
        val p = prob?.toInt() ?: return null
        return when {
            p >= 60 -> "Recomendación: lleva paraguas"
            p >= 30 -> "Posibles lluvias"
            else -> "Baja probabilidad de lluvia"
        }
    }

    fun ventanaSeca(probPorHora: List<Double>?): String? {
        val base = probPorHora ?: return null
        if (base.isEmpty()) return null

        val arr = base.take(24)

        var bestStart = -1
        var bestEnd = -1
        var currStart = -1

        arr.forEachIndexed { i, p ->
            if (p < 20) {
                if (currStart == -1) currStart = i
            } else {
                if (currStart != -1 && (i - currStart) > (bestEnd - bestStart)) {
                    bestStart = currStart
                    bestEnd = i
                }
                currStart = -1
            }
        }

        if (currStart != -1 && (arr.size - currStart) > (bestEnd - bestStart)) {
            bestStart = currStart
            bestEnd = arr.size
        }

        if (bestStart == -1 || bestEnd - bestStart < 2) return null

        return "Ventana seca: ${bestStart}:00–${bestEnd}:00"
    }

    fun sensacionTermicaActual(temp: Double?, feelsLike: Double?): String? {
        if (temp == null || feelsLike == null) return null

        val diff = feelsLike - temp
        return when {
            diff >= 3 -> "Se siente más caluroso de lo que marca el termómetro"
            diff <= -3 -> "Se siente más frío de lo que marca el termómetro"
            else -> "La sensación térmica es similar a la temperatura"
        }
    }

    fun vientoHoy(windSpeed: Double?): String? {
        val w = windSpeed ?: return null
        val wInt = w.toInt()

        return when {
            wInt >= 35 -> "Viento fuerte, considera chamarra o rompevientos"
            wInt >= 20 -> "Viento moderado, podría sentirse más fresco"
            wInt >= 5  -> "Viento ligero, condiciones agradables"
            else       -> "Apenas hay viento, ambiente tranquilo"
        }
    }

    // -------- SEMANA: POR DÍA --------

    fun recoDia(min: Double?, max: Double?, rain: Double?): String {
        val tmin = min?.toInt() ?: 0
        val tmax = max?.toInt() ?: 0
        val pr = rain?.toInt() ?: 0

        val partes = mutableListOf<String>()

        if (tmax - tmin >= 10) {
            partes.add("rango amplio")
        } else {
            partes.add("rango estable")
        }

        when {
            pr >= 60 -> partes.add("lluvia alta, lleva paraguas")
            pr >= 30 -> partes.add("lluvia moderada")
            else -> partes.add("lluvia baja")
        }

        val avg = (tmin + tmax) / 2
        when {
            avg <= 10 -> partes.add("día frío")
            avg <= 20 -> partes.add("día fresco")
            avg <= 28 -> partes.add("clima agradable")
            else -> partes.add("día caluroso")
        }

        return partes.joinToString(" · ")
    }

    // -------- SEMANA: RESUMEN GENERAL --------

    fun rangoTermicoSemana(
        mins: List<Double>?,
        maxs: List<Double>?
    ): String? {
        if (mins.isNullOrEmpty() || maxs.isNullOrEmpty()) return null

        val minGlobal = mins.minOrNull() ?: return null
        val maxGlobal = maxs.maxOrNull() ?: return null

        return "En la semana: ${minGlobal.toInt()}–${maxGlobal.toInt()}°"
    }

    fun lluviaSemana(rains: List<Double>?): String? {
        val base = rains ?: return null
        if (base.isEmpty()) return null

        val maxRain = (base.maxOrNull() ?: 0.0).toInt()
        val diasLluviaAlta = base.count { it >= 60.0 }
        val diasLluviaMedia = base.count { it in 30.0..59.9 }

        return when {
            diasLluviaAlta >= 3 ->
                "Semana muy lluviosa, varios días con lluvia alta"
            diasLluviaAlta >= 1 ->
                "Habrá algunos días con lluvia alta, revisa el pronóstico diario"
            (diasLluviaMedia + diasLluviaAlta) >= 3 ->
                "Lluvias moderadas en varios días de la semana"
            maxRain <= 20 ->
                "Semana mayormente seca, lluvias poco probables"
            else ->
                "Lluvia aislada en algunos días de la semana"
        }
    }

    fun consejoGeneralSemana(
        mins: List<Double>?,
        maxs: List<Double>?
    ): String? {
        if (mins.isNullOrEmpty() || maxs.isNullOrEmpty()) return null

        val n = kotlin.math.min(mins.size, maxs.size)
        if (n == 0) return null

        val promedioSemanal = (0 until n)
            .map { i -> (mins[i] + maxs[i]) / 2.0 }
            .average()

        return when {
            promedioSemanal <= 10 ->
                "Semana fría: privilegia ropa abrigadora y varias capas."
            promedioSemanal <= 20 ->
                "Semana fresca: usa capas ligeras y suéteres medianos."
            promedioSemanal <= 28 ->
                "Semana templada: ropa ligera, una capa extra en mañanas y noches."
            else ->
                "Semana calurosa: ropa muy ligera y buena hidratación."
        }
    }
}
