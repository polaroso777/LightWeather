package com.example.lightweather

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var prefs: SharedPreferences

    // VM compartido con Today/Week
    private val vm by activityViewModels<TodayVM>()

    // Views
    private lateinit var etFavoriteLocation: EditText
    private lateinit var btnSaveFavoriteLocation: Button
    private lateinit var btnUseCurrentLocation: Button

    private lateinit var switchFollowSystemTheme: SwitchCompat
    private lateinit var layoutManualTheme: LinearLayout
    private lateinit var rgThemeOptions: RadioGroup
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeDark: RadioButton

    private lateinit var tvLocationPermissionStatus: TextView
    private lateinit var btnManagePermissions: Button

    private lateinit var btnResetSettings: Button
    private lateinit var tvAppVersion: TextView

    // Lista de ciudades guardadas
    private lateinit var rvSavedLocations: RecyclerView
    private lateinit var tvEmptyLocations: TextView
    private lateinit var savedLocationsAdapter: SavedLocationsAdapter
    private val savedLocations = mutableListOf<SavedLocation>()

    companion object {
        private const val PREFS_NAME = "lightweather"
        private const val KEY_FAVORITE_LOCATION = "place_name"

        private const val KEY_FOLLOW_SYSTEM_THEME = "follow_system_theme"
        private const val KEY_THEME_MODE = "theme_mode"

        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"

        private const val KEY_SAVED_LOCATIONS = "saved_locations"
        private const val KEY_LAT = "lat"
        private const val KEY_LON = "lon"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        bindViews(view)
        setupRecyclerView()
        loadInitialState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateLocationPermissionStatus()
        val favLocation = prefs.getString(KEY_FAVORITE_LOCATION, "") ?: ""
        etFavoriteLocation.setText(favLocation)

        loadSavedLocationsFromPrefs()
        refreshSavedLocationsUI()
    }

    private fun bindViews(view: View) {
        etFavoriteLocation = view.findViewById(R.id.etFavoriteLocation)
        btnSaveFavoriteLocation = view.findViewById(R.id.btnSaveFavoriteLocation)
        btnUseCurrentLocation = view.findViewById(R.id.btnUseCurrentLocation)

        switchFollowSystemTheme = view.findViewById(R.id.switchFollowSystemTheme)
        layoutManualTheme = view.findViewById(R.id.layoutManualTheme)
        rgThemeOptions = view.findViewById(R.id.rgThemeOptions)
        rbThemeLight = view.findViewById(R.id.rbThemeLight)
        rbThemeDark = view.findViewById(R.id.rbThemeDark)

        tvLocationPermissionStatus = view.findViewById(R.id.tvLocationPermissionStatus)
        btnManagePermissions = view.findViewById(R.id.btnManagePermissions)

        btnResetSettings = view.findViewById(R.id.btnResetSettings)
        tvAppVersion = view.findViewById(R.id.tvAppVersion)

        rvSavedLocations = view.findViewById(R.id.rvSavedLocations)
        tvEmptyLocations = view.findViewById(R.id.tvEmptyLocations)
    }

    private fun setupRecyclerView() {
        savedLocationsAdapter = SavedLocationsAdapter(
            items = savedLocations,
            onUseClicked = { location -> setCurrentLocationFromSaved(location) },
            onDeleteClicked = { location -> removeSavedLocation(location) }
        )

        rvSavedLocations.layoutManager = LinearLayoutManager(requireContext())
        rvSavedLocations.adapter = savedLocationsAdapter
    }

    private fun loadInitialState() {
        val favLocation = prefs.getString(KEY_FAVORITE_LOCATION, "") ?: ""
        etFavoriteLocation.setText(favLocation)

        val followSystem = prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, true)
        switchFollowSystemTheme.isChecked = followSystem
        setManualThemeEnabled(!followSystem)

        val savedTheme = prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
        when (savedTheme) {
            THEME_DARK -> rbThemeDark.isChecked = true
            else -> rbThemeLight.isChecked = true
        }

        applyThemeFromPrefs()
        updateLocationPermissionStatus()
        setAppVersionText()

        loadSavedLocationsFromPrefs()
        refreshSavedLocationsUI()
    }

    private fun setupListeners() {
        // Guardar ciudad manualmente
        btnSaveFavoriteLocation.setOnClickListener {
            val query = etFavoriteLocation.text.toString().trim()

            if (query.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Escribe una ciudad antes de guardar.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val result = LocationResolver.resolveAndSaveLocation(requireContext(), query)

            if (result == null) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo encontrar esa ubicación.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                etFavoriteLocation.setText(result.displayName)
                Toast.makeText(
                    requireContext(),
                    "Ubicación guardada.",
                    Toast.LENGTH_SHORT
                ).show()

                // 🔄 recarga clima con la nueva ubicación
                vm.load(result.latitude, result.longitude, result.displayName)

                addOrUpdateSavedLocation(
                    SavedLocation(
                        name = result.displayName,
                        lat = result.latitude,
                        lon = result.longitude
                    )
                )
            }
        }

        // Usar ubicación actual (GPS)
        btnUseCurrentLocation.setOnClickListener {
            val main = activity as? MainActivity
            if (main == null) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo acceder a la actividad principal.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                main.requestLocationFromSettings()
                Toast.makeText(
                    requireContext(),
                    "Obteniendo ubicación actual...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        switchFollowSystemTheme.setOnCheckedChangeListener { _, isChecked ->
            saveFollowSystemTheme(isChecked)
            setManualThemeEnabled(!isChecked)
            applyThemeFromPrefs()
        }

        rgThemeOptions.setOnCheckedChangeListener { _, checkedId ->
            if (!switchFollowSystemTheme.isChecked) {
                when (checkedId) {
                    R.id.rbThemeLight -> saveThemeMode(THEME_LIGHT)
                    R.id.rbThemeDark -> saveThemeMode(THEME_DARK)
                }
                applyThemeFromPrefs()
            }
        }

        btnManagePermissions.setOnClickListener {
            openAppSettings()
        }

        btnResetSettings.setOnClickListener {
            resetSettings()
        }
    }

    // ---------- Tema ----------

    private fun saveFollowSystemTheme(follow: Boolean) {
        prefs.edit()
            .putBoolean(KEY_FOLLOW_SYSTEM_THEME, follow)
            .apply()
    }

    private fun saveThemeMode(mode: String) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }

    private fun applyThemeFromPrefs() {
        val followSystem = prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, true)
        if (followSystem) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            val mode = prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
            val nightMode = when (mode) {
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    private fun setManualThemeEnabled(enabled: Boolean) {
        layoutManualTheme.isEnabled = enabled
        for (i in 0 until rgThemeOptions.childCount) {
            rgThemeOptions.getChildAt(i).isEnabled = enabled
        }
    }

    // ---------- Permisos ----------

    private fun updateLocationPermissionStatus() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = hasFine || hasCoarse

        tvLocationPermissionStatus.text = if (granted) {
            "Ubicación: concedido"
        } else {
            "Ubicación: denegado"
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // ---------- Reset ----------

    private fun resetSettings() {
        prefs.edit().clear().apply()

        etFavoriteLocation.setText("")
        switchFollowSystemTheme.isChecked = true
        setManualThemeEnabled(false)
        rbThemeLight.isChecked = true

        savedLocations.clear()
        refreshSavedLocationsUI()

        applyThemeFromPrefs()
        updateLocationPermissionStatus()

        Toast.makeText(requireContext(), "Ajustes restablecidos", Toast.LENGTH_SHORT).show()
    }

    // ---------- Versión ----------

    private fun setAppVersionText() {
        try {
            val pm = requireContext().packageManager
            val pInfo = pm.getPackageInfo(requireContext().packageName, 0)
            val versionName = pInfo.versionName ?: "1.0"
            tvAppVersion.text = "LightWeather • Versión $versionName"
        } catch (e: Exception) {
            tvAppVersion.text = "LightWeather"
        }
    }

    // ---------- Ciudades guardadas ----------

    data class SavedLocation(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    private fun loadSavedLocationsFromPrefs() {
        savedLocations.clear()
        val stored = prefs.getStringSet(KEY_SAVED_LOCATIONS, emptySet()) ?: emptySet()
        for (raw in stored) {
            decodeSavedLocation(raw)?.let { savedLocations.add(it) }
        }
    }

    private fun saveSavedLocationsToPrefs() {
        val set = savedLocations.map { encodeSavedLocation(it) }.toSet()
        prefs.edit()
            .putStringSet(KEY_SAVED_LOCATIONS, set)
            .apply()
    }

    private fun refreshSavedLocationsUI() {
        tvEmptyLocations.visibility =
            if (savedLocations.isEmpty()) View.VISIBLE else View.GONE
        savedLocationsAdapter.notifyDataSetChanged()
    }

    private fun addOrUpdateSavedLocation(location: SavedLocation) {
        val index = savedLocations.indexOfFirst { it.name == location.name }
        if (index >= 0) {
            savedLocations[index] = location
        } else {
            savedLocations.add(location)
        }
        saveSavedLocationsToPrefs()
        refreshSavedLocationsUI()
    }

    private fun removeSavedLocation(location: SavedLocation) {
        val index = savedLocations.indexOfFirst {
            it.name == location.name &&
                    it.lat == location.lat &&
                    it.lon == location.lon
        }
        if (index >= 0) {
            savedLocations.removeAt(index)
            saveSavedLocationsToPrefs()
            refreshSavedLocationsUI()
        }
    }

    private fun setCurrentLocationFromSaved(location: SavedLocation) {
        prefs.edit()
            .putString(KEY_LAT, location.lat.toString())
            .putString(KEY_LON, location.lon.toString())
            .putString(KEY_FAVORITE_LOCATION, location.name)
            .apply()

        etFavoriteLocation.setText(location.name)

        // 🔄 recarga clima con esa ciudad
        vm.load(location.lat, location.lon, location.name)

        Toast.makeText(
            requireContext(),
            "Ubicación actual: ${location.name}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun encodeSavedLocation(location: SavedLocation): String {
        return "${location.name}|||${location.lat}|||${location.lon}"
    }

    private fun decodeSavedLocation(raw: String): SavedLocation? {
        val parts = raw.split("|||")
        if (parts.size != 3) return null

        val name = parts[0]
        val lat = parts[1].toDoubleOrNull()
        val lon = parts[2].toDoubleOrNull()

        if (lat == null || lon == null) return null

        return SavedLocation(name, lat, lon)
    }

    // ---------- Adapter ----------

    private class SavedLocationsAdapter(
        private val items: MutableList<SavedLocation>,
        private val onUseClicked: (SavedLocation) -> Unit,
        private val onDeleteClicked: (SavedLocation) -> Unit
    ) : RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLocationName: TextView = view.findViewById(R.id.tvLocationName)
            val tvLocationInfo: TextView = view.findViewById(R.id.tvLocationInfo)
            val btnSelect: Button = view.findViewById(R.id.btnSelectLocation)
            val btnDelete: Button = view.findViewById(R.id.btnDeleteLocation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_location, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvLocationName.text = item.name
            holder.tvLocationInfo.text = "Lat: ${item.lat}, Lon: ${item.lon}"

            holder.btnSelect.setOnClickListener {
                onUseClicked(item)
            }

            holder.btnDelete.setOnClickListener {
                onDeleteClicked(item)
            }
        }
    }
}
