package com.example.lightweather

object IconMapper {

    // -------------------------------------------------------------------------
    // 1) CATEGORÍAS DE CÓDIGOS (Open-Meteo)
    // -------------------------------------------------------------------------

    private val CLEAR = setOf(0)
    private val PARTLY_CLOUDY = setOf(1, 2)
    private val CLOUDY = setOf(3)
    private val FOG = setOf(45, 48)

    private val DRIZZLE = setOf(51, 53, 55, 56, 57)
    private val RAIN = setOf(61, 63, 65)
    private val FREEZING_RAIN = setOf(66, 67)

    private val SNOW = setOf(71, 73, 75, 77)
    private val SNOW_SHOWERS = setOf(85, 86)

    private val SHOWERS = setOf(80, 81, 82)

    private val THUNDERSTORM = setOf(95)
    private val THUNDER_HAIL = setOf(96, 97, 98, 99)

    // -------------------------------------------------------------------------
    // 2) HELPERS DE LÓGICA
    // -------------------------------------------------------------------------

    private fun isStrongWind(w: Double?) = (w ?: 0.0) >= 50
    private fun isWindy(w: Double?) = (w ?: 0.0) >= 25
    private fun isCold(t: Double?) = (t ?: 0.0) <= 5
    private fun isHot(t: Double?) = (t ?: 0.0) >= 30

    // -------------------------------------------------------------------------
    // 3) FUNCIÓN PRINCIPAL (HORA A ÍCONO)
    // -------------------------------------------------------------------------

    fun iconForHour(
        weatherCode: Int?,
        windSpeed: Double?,
        temp: Double?
    ): Int {

        val code = weatherCode ?: -1

        // 1) VIENTO EXTREMO primero
        if (isStrongWind(windSpeed)) {
            return R.drawable.wi_strong_wind
        }

        // 2) CATEGORÍAS
        return when {

            // ---------------- CLARO ----------------
            code in CLEAR -> {
                when {
                    isHot(temp) -> R.drawable.wi_hot
                    isCold(temp) -> R.drawable.wi_snowflake_cold
                    isWindy(windSpeed) -> R.drawable.wi_windy
                    else -> R.drawable.wi_day_sunny
                }
            }

            // ---------- PARCIALMENTE NUBLADO ----------
            code in PARTLY_CLOUDY -> {
                when {
                    isWindy(windSpeed) -> R.drawable.wi_cloudy_windy
                    isCold(temp) -> R.drawable.wi_snowflake_cold
                    else -> R.drawable.wi_day_cloudy
                }
            }

            // ---------------- NUBLADO ----------------
            code in CLOUDY -> {
                if (isWindy(windSpeed))
                    R.drawable.wi_cloudy_windy
                else
                    R.drawable.wi_cloudy
            }

            // ---------------- NIEBLA ----------------
            code in FOG -> R.drawable.wi_fog

            // ---------------- LLovizna ----------------
            code in DRIZZLE -> R.drawable.wi_sprinkle

            // ---------------- Lluvia ----------------
            code in RAIN -> R.drawable.wi_rain

            // -------------- Lluvia helada --------------
            code in FREEZING_RAIN -> R.drawable.wi_raindrops

            // ------------------- Nieve -------------------
            code in SNOW || code in SNOW_SHOWERS -> R.drawable.wi_snow

            // ----------------- Chubascos -----------------
            code in SHOWERS -> R.drawable.wi_showers

            // ---------------- Tormenta ------------------
            code in THUNDERSTORM || code in THUNDER_HAIL -> R.drawable.wi_storm_showers

            // ---------------- Fallback ------------------
            else -> {
                when {
                    isHot(temp) -> R.drawable.wi_hot
                    isCold(temp) -> R.drawable.wi_snowflake_cold
                    isWindy(windSpeed) -> R.drawable.wi_windy
                    else -> R.drawable.wi_thermometer_exterior
                }
            }
        }
    }
}
