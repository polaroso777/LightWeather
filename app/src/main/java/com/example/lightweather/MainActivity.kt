package com.example.lightweather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lightweather.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // VM compartido con TodayFragment y WeekFragment
    private val todayVM: TodayVM by viewModels()

    // Google location client
    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    // Fragment actualmente visible
    private lateinit var activeFragment: Fragment

    companion object {
        private const val TAG_TODAY = "today_fragment"
        private const val TAG_WEEK = "week_fragment"
        private const val TAG_SETTINGS = "settings_fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fm = supportFragmentManager

        if (savedInstanceState == null) {
            // Primera vez: creamos solo el fragmento de "Hoy"
            val todayFragment = TodayFragment()
            fm.beginTransaction()
                .add(R.id.fragment_container, todayFragment, TAG_TODAY)
                .commit()

            activeFragment = todayFragment
        } else {
            // La actividad se recreó (ej: cambio de tema)
            // Recuperamos el fragmento actualmente visible
            val current = fm.findFragmentById(R.id.fragment_container)
            activeFragment = current ?: fm.findFragmentByTag(TAG_TODAY) ?: TodayFragment().also {
                fm.beginTransaction()
                    .add(R.id.fragment_container, it, TAG_TODAY)
                    .commit()
            }
        }

        // Navegación inferior
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_today -> navigateTo(TAG_TODAY)
                R.id.menu_week -> navigateTo(TAG_WEEK)
                R.id.menu_settings -> navigateTo(TAG_SETTINGS)
            }
            true
        }

        // Aseguramos que arranque marcado en "Hoy"
        if (binding.bottomNav.selectedItemId == 0) {
            binding.bottomNav.selectedItemId = R.id.menu_today
        }

        // Permisos de ubicación
        if (hasLocationPermission()) {
            fetchLocationAndSave()
        } else {
            requestLocationPermission()
        }
    }

    private fun navigateTo(tag: String) {
        val fm = supportFragmentManager

        // Si por alguna razón aún no está inicializado, lo inicializamos a "Hoy"
        if (!::activeFragment.isInitialized) {
            val fallback = fm.findFragmentByTag(TAG_TODAY) ?: TodayFragment().also {
                fm.beginTransaction()
                    .add(R.id.fragment_container, it, TAG_TODAY)
                    .commit()
            }
            activeFragment = fallback
        }

        // Buscamos el fragmento destino por tag
        var target = fm.findFragmentByTag(tag)

        // Si no existe todavía, lo creamos y lo añadimos oculto/mostrado correctamente
        if (target == null) {
            target = when (tag) {
                TAG_TODAY -> TodayFragment()
                TAG_WEEK -> WeekFragment()
                TAG_SETTINGS -> SettingsFragment()
                else -> return
            }

            fm.beginTransaction()
                .add(R.id.fragment_container, target, tag)
                .hide(activeFragment)
                .show(target)
                .commit()

            activeFragment = target
            return
        }

        // Si ya estamos en ese fragment, no hacemos nada
        if (target === activeFragment) return

        // Ocultamos el actual y mostramos el destino
        fm.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()

        activeFragment = target
    }

    // ---------------- Permisos ----------------

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

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndSave()
        }
    }

    // 👉 llamado desde SettingsFragment cuando tocas "Usar ubicación actual"
    fun requestLocationFromSettings() {
        if (hasLocationPermission()) {
            fetchLocationAndSave()
        } else {
            requestLocationPermission()
        }
    }

    // ---------------- Ubicación + recarga de clima ----------------

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

                    val geocoder = Geocoder(this, Locale.getDefault())
                    val placeName = try {
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        val address = addresses?.firstOrNull()

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

                    // 🔄 recarga clima para Today/Week con la nueva ubicación
                    todayVM.load(loc.latitude, loc.longitude, placeName)
                }
            }
    }
}
