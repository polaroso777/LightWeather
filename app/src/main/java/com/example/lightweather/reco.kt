package com.example.lightweather

/**
 * Reglas climáticas desacopladas de UI.
 * Todas las funciones devuelven estructuras de datos,
 * NO textos finales.
 */
object Reco {

    // -----------------------------------------------------
    // DATA CLASSES (RESULTADOS ESTRUCTURADOS)
    // -----------------------------------------------------

    data class RangoTermico(val min: Int, val max: Int)
    data class Paraguas(val recomendacion: TipoParaguas)
    data class VentanaSeca(val startHour: Int, val endHour: Int)
    data class SensacionTermica(val tipo: TipoSensacion)
    data class Viento(val tipo: TipoViento)

    enum class TipoParaguas { ALTO, MEDIO, BAJO }
    enum class TipoSensacion { MAS_CALOR, MAS_FRIO, SIMILAR }
    enum class TipoViento { FUERTE, MODERADO, LIGERO, CALMO }

    data class RecoDia(
        val rango: RangoTipo,
        val lluvia: LluviaTipo,
        val clima: ClimaTipo
    )

    enum class RangoTipo { AMPLIO, ESTABLE }
    enum class LluviaTipo { ALTA, MEDIA, BAJA }
    enum class ClimaTipo { FRIO, FRESCO, AGRADABLE, CALUROSO }

    data class ResumenSemana(
        val rango: RangoTermico?,
        val lluvia: LluviaTipoSemana?,
        val consejo: ConsejoSemana?
    )

    enum class LluviaTipoSemana {
        MUY_LLUVIOSA,
        LLUVIA_ALTA,
        LLUVIA_VARIADA,
        MAYORMENTE_SECA,
        AISLADA
    }


    enum class ConsejoSemana {
        FRIA,
        FRESCA,
        TEMPLADA,
        CALUROSA
    }

    // -----------------------------------------------------
    // HOY — LÓGICA PURA
    // -----------------------------------------------------

    fun rangoTermico(min: Double?, max: Double?): RangoTermico? {
        if (min == null || max == null) return null
        return RangoTermico(min.toInt(), max.toInt())
    }

    fun paraguas(prob: Double?): Paraguas? {
        val p = prob?.toInt() ?: return null

        val tipo = when {
            p >= 60 -> TipoParaguas.ALTO
            p >= 30 -> TipoParaguas.MEDIO
            else -> TipoParaguas.BAJO
        }

        return Paraguas(tipo)
    }

    fun ventanaSeca(probPorHora: List<Double>?): VentanaSeca? {
        val arr = probPorHora ?: return null
        if (arr.isEmpty()) return null

        val hours = arr.take(24)

        var bestStart = -1
        var bestEnd = -1
        var currentStart = -1

        hours.forEachIndexed { index, p ->
            if (p < 20) {
                if (currentStart == -1) currentStart = index
            } else {
                if (currentStart != -1 && (index - currentStart) > (bestEnd - bestStart)) {
                    bestStart = currentStart
                    bestEnd = index
                }
                currentStart = -1
            }
        }

        if (currentStart != -1 && (hours.size - currentStart) > (bestEnd - bestStart)) {
            bestStart = currentStart
            bestEnd = hours.size
        }

        if (bestStart == -1 || (bestEnd - bestStart) < 2) return null

        return VentanaSeca(bestStart, bestEnd)
    }

    fun sensacionTermicaActual(temp: Double?, feelsLike: Double?): SensacionTermica? {
        if (temp == null || feelsLike == null) return null

        val diff = feelsLike - temp
        val tipo = when {
            diff >= 3 -> TipoSensacion.MAS_CALOR
            diff <= -3 -> TipoSensacion.MAS_FRIO
            else -> TipoSensacion.SIMILAR
        }

        return SensacionTermica(tipo)
    }

    fun vientoHoy(windSpeed: Double?): Viento? {
        val w = windSpeed?.toInt() ?: return null

        val tipo = when {
            w >= 35 -> TipoViento.FUERTE
            w >= 20 -> TipoViento.MODERADO
            w >= 5  -> TipoViento.LIGERO
            else    -> TipoViento.CALMO
        }

        return Viento(tipo)
    }

    // -----------------------------------------------------
    // SEMANA — POR DÍA
    // -----------------------------------------------------

    fun recoDia(min: Double?, max: Double?, rain: Double?): RecoDia? {
        if (min == null || max == null || rain == null) return null

        val tmin = min.toInt()
        val tmax = max.toInt()
        val pr = rain.toInt()

        val rango = if (tmax - tmin >= 10) RangoTipo.AMPLIO else RangoTipo.ESTABLE

        val lluvia = when {
            pr >= 60 -> LluviaTipo.ALTA
            pr >= 30 -> LluviaTipo.MEDIA
            else -> LluviaTipo.BAJA
        }

        val avg = (tmin + tmax) / 2
        val clima = when {
            avg <= 10 -> ClimaTipo.FRIO
            avg <= 20 -> ClimaTipo.FRESCO
            avg <= 28 -> ClimaTipo.AGRADABLE
            else -> ClimaTipo.CALUROSO
        }

        return RecoDia(rango, lluvia, clima)
    }

    // -----------------------------------------------------
    // SEMANA — RESUMEN
    // -----------------------------------------------------

    fun resumenSemana(
        mins: List<Double>?,
        maxs: List<Double>?,
        rains: List<Double>?
    ): ResumenSemana {

        val rango = if (!mins.isNullOrEmpty() && !maxs.isNullOrEmpty()) {
            RangoTermico(
                mins.minOf { it.toInt() },
                maxs.maxOf { it.toInt() }
            )
        } else null

        val lluvia = if (rains.isNullOrEmpty()) null else {
            val diasAlta = rains.count { it >= 60 }
            val diasMedia = rains.count { it in 30.0..59.9 }
            val maxRain = rains.maxOrNull()?.toInt() ?: 0

            when {
                diasAlta >= 3 ->
                    LluviaTipoSemana.MUY_LLUVIOSA

                diasAlta >= 1 ->
                    LluviaTipoSemana.LLUVIA_ALTA

                (diasMedia + diasAlta) >= 3 ->
                    LluviaTipoSemana.LLUVIA_VARIADA

                maxRain <= 20 ->
                    LluviaTipoSemana.MAYORMENTE_SECA

                else ->
                    LluviaTipoSemana.AISLADA
            }
        }


        val consejo = if (!mins.isNullOrEmpty() && !maxs.isNullOrEmpty()) {
            val n = minOf(mins.size, maxs.size)
            if (n == 0) null
            else {
                val promedio = (0 until n)
                    .map { i -> (mins[i] + maxs[i]) / 2 }
                    .average()

                when {
                    promedio <= 10 -> ConsejoSemana.FRIA
                    promedio <= 20 -> ConsejoSemana.FRESCA
                    promedio <= 28 -> ConsejoSemana.TEMPLADA
                    else -> ConsejoSemana.CALUROSA
                }
            }
        } else null

        return ResumenSemana(rango, lluvia, consejo)
    }
}
