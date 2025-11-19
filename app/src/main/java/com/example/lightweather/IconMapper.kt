package com.example.lightweather

object IconMapper {
    fun iconForHour(
        weatherCode: Int?,
        windSpeed: Double?,
        temp: Double?
    ): Int {

        val wind = windSpeed ?: 0.0
        val t = temp ?: 0.0

        // VIENTO FUERTE
        if (wind >= 50) {
            return R.drawable.wi_strong_wind
        }

        // Interpretación según weather_code
        when (weatherCode) {

            // Claro
            0 -> {
                return when {
                    t >= 30 -> R.drawable.wi_hot
                    t <= 5  -> R.drawable.wi_snowflake_cold
                    wind >= 25 -> R.drawable.wi_windy
                    else -> R.drawable.wi_thermometer // neutro
                }
            }

            // Parcialmente nublado
            1, 2 -> {
                return when {
                    wind >= 25 -> R.drawable.wi_cloudy_windy
                    t <= 5     -> R.drawable.wi_snowflake_cold
                    else       -> R.drawable.wi_cloudy_windy // mejor que thermometer
                }
            }

            // Nublado
            3 -> {
                return when {
                    wind >= 25 -> R.drawable.wi_cloudy_windy
                    else       -> R.drawable.wi_cloudy_windy
                }
            }

            // Neblina / Niebla
            45, 48 -> {
                return R.drawable.wi_fog
            }

            // Llovizna ligera a fuerte
            51, 53, 55, 56, 57 -> {
                return R.drawable.wi_sprinkle
            }

            // Lluvia ligera / moderada / fuerte
            61, 63, 65 -> {
                return R.drawable.wi_rain
            }

            // Lluvia helada
            66, 67 -> {
                return R.drawable.wi_raindrops
            }

            // Nieve / granizo
            71, 73, 75, 77 -> {
                return R.drawable.wi_snowflake_cold
            }

            // Chubascos
            80, 81, 82 -> {
                return R.drawable.wi_showers
            }

            // Nevadas fuertes
            85, 86 -> {
                return R.drawable.wi_snowflake_cold
            }

            // Tormenta
            95 -> {
                return R.drawable.wi_storm_showers
            }

            // Tormenta con granizo
            96, 97, 98, 99 -> {
                return R.drawable.wi_storm_showers
            }
        }

        // Fallback
        return when {
            t >= 30 -> R.drawable.wi_hot
            t <= 5  -> R.drawable.wi_snowflake_cold
            wind >= 25 -> R.drawable.wi_windy
            else -> R.drawable.wi_thermometer_exterior
        }
    }
}
