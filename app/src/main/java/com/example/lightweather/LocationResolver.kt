package com.example.lightweather

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.util.Locale

data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)

object LocationResolver {

    private const val PREFS_NAME = "lightweather"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_PLACE_NAME = "place_name"

    private const val TAG = "LocationResolver"

    /**
     * Intenta resolver un nombre de ciudad a coordenadas y guardarlo en SharedPreferences.
     * @param context Contexto de la app
     * @param query Texto introducido por el usuario (ej. "Ciudad de México, México")
     * @return ResolvedLocation si se pudo resolver, null si algo falló (y NO modifica prefs).
     */
    fun resolveAndSaveLocation(context: Context, query: String): ResolvedLocation? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            Log.w(TAG, "Query vacío, no se intenta resolver.")
            return null
        }

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(trimmedQuery, 1)

            if (results.isNullOrEmpty()) {
                Log.w(TAG, "No se encontró ubicación para: $trimmedQuery")
                null
            } else {
                val addr = results[0]
                val lat = addr.latitude
                val lon = addr.longitude

                val locality = addr.locality ?: addr.subAdminArea
                val admin = addr.adminArea
                val country = addr.countryName

                // Nombre “bonito” para mostrar
                val displayName = buildString {
                    if (!locality.isNullOrBlank()) append(locality)
                    if (!admin.isNullOrBlank() && admin != locality) {
                        if (isNotEmpty()) append(", ")
                        append(admin)
                    }
                    if (!country.isNullOrBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(country)
                    }
                }.ifBlank { trimmedQuery }

                // Guardamos TODO como String, igual que MainActivity
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_LAT, lat.toString())
                    .putString(KEY_LON, lon.toString())
                    .putString(KEY_PLACE_NAME, displayName)
                    .apply()

                Log.d(TAG, "Ubicación resuelta y guardada: $displayName ($lat, $lon)")

                ResolvedLocation(
                    latitude = lat,
                    longitude = lon,
                    displayName = displayName
                )
            }
        } catch (e: Exception) {
            // Si algo truena, NO tocamos SharedPreferences
            Log.e(TAG, "Error al resolver ubicación para: $trimmedQuery", e)
            null
        }
    }

    /**
     * Solo resuelve la ciudad a coordenadas sin guardar en SharedPreferences.
     */
    fun resolveLocation(context: Context, query: String): ResolvedLocation? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return null

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(trimmedQuery, 1)

            if (results.isNullOrEmpty()) {
                null
            } else {
                val addr = results[0]
                val lat = addr.latitude
                val lon = addr.longitude

                val locality = addr.locality ?: addr.subAdminArea
                val admin = addr.adminArea
                val country = addr.countryName

                val displayName = buildString {
                    if (!locality.isNullOrBlank()) append(locality)
                    if (!admin.isNullOrBlank() && admin != locality) {
                        if (isNotEmpty()) append(", ")
                        append(admin)
                    }
                    if (!country.isNullOrBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(country)
                    }
                }.ifBlank { trimmedQuery }

                ResolvedLocation(
                    latitude = lat,
                    longitude = lon,
                    displayName = displayName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en resolveLocation para: $trimmedQuery", e)
            null
        }
    }
}
