package com.example.lightweather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lightweather.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Cliente de ubicación de Google
    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fragment inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TodayFragment())
                .commit()
        }

        // Bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            val frag = when (item.itemId) {
                R.id.menu_today -> TodayFragment()
                R.id.menu_week -> WeekFragment()
                R.id.menu_settings -> SettingsFragment()
                else -> TodayFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, frag)
                .commit()
            true
        }

        // Permisos de ubicación
        if (hasLocationPermission()) {
            // Ya hay permiso → obtener ubicación y guardarla
            fetchLocationAndSave()
        } else {
            // No hay permiso → solicitarlo
            requestLocationPermission()
        }
    }

    // ---------------- PERMISOS DE UBICACIÓN ----------------

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return fine == PackageManager.PERMISSION_GRANTED ||
                coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // Ya dieron permiso → obtener ubicación
                fetchLocationAndSave()
            } else {
                // Negó permisos: aquí podrías dejar CDMX fija o mostrar un mensaje
            }
        }
    }

    // ---------------- OBTENER UBICACIÓN Y GUARDARLA ----------------

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    private fun fetchLocationAndSave() {
        if (!hasLocationPermission()) return

        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val prefs = getSharedPreferences("lightweather", MODE_PRIVATE)

                    // --- Reverse geocoding: lat/lon -> nombre amigable ---
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val placeName = try {
                        val addressList = geocoder.getFromLocation(
                            loc.latitude,
                            loc.longitude,
                            1
                        )
                        val address = addressList?.firstOrNull()

                        val city = address?.locality ?: address?.subAdminArea ?: ""
                        val state = address?.adminArea ?: ""

                        when {
                            city.isNotBlank() && state.isNotBlank() -> "$city, $state"
                            city.isNotBlank() -> city
                            state.isNotBlank() -> state
                            else -> "Ubicación actual"
                        }
                    } catch (e: Exception) {
                        "Ubicación actual"
                    }

                    prefs.edit()
                        .putString("lat", loc.latitude.toString())
                        .putString("lon", loc.longitude.toString())
                        .putString("place_name", placeName)
                        .apply()

                    // Refrescar TodayFragment para que use coords + nombre nuevos
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TodayFragment())
                        .commit()
                } else {
                    // Si es null, el dispositivo aún no tiene ubicación: se queda CDMX por defecto
                }
            }
            .addOnFailureListener {
                // Aquí puedes loguear o ignorar; no rompe nada
            }
    }
}