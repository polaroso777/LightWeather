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

    // Fragments creados una sola vez
    private val todayFragment = TodayFragment()
    private val weekFragment = WeekFragment()
    private val settingsFragment = SettingsFragment()

    private lateinit var activeFragment: androidx.fragment.app.Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, settingsFragment).hide(settingsFragment)
                .add(R.id.fragment_container, weekFragment).hide(weekFragment)
                .add(R.id.fragment_container, todayFragment)
                .commit()

            activeFragment = todayFragment
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_today   -> navigateTo(todayFragment)
                R.id.menu_week    -> navigateTo(weekFragment)
                R.id.menu_settings-> navigateTo(settingsFragment)
            }
            true
        }

        if (hasLocationPermission()) {
            fetchLocationAndSave()
        } else {
            requestLocationPermission()
        }
    }

    private fun navigateTo(target: androidx.fragment.app.Fragment) {
        if (target == activeFragment) return

        supportFragmentManager.beginTransaction()
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
