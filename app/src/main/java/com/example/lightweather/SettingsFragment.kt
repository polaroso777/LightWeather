package com.example.lightweather

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("lightweather", Context.MODE_PRIVATE)

        val etLat = view.findViewById<EditText>(R.id.etLat)
        val etLon = view.findViewById<EditText>(R.id.etLon)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // -------- Cargar valores guardados --------
        etLat.setText(prefs.getString("lat", "19.4326"))
        etLon.setText(prefs.getString("lon", "-99.1332"))

        // -------- Guardar nueva ubicación --------
        btnSave.setOnClickListener {
            val latText = etLat.text.toString()
            val lonText = etLon.text.toString()

            if (latText.isBlank() || lonText.isBlank()) {
                Toast.makeText(requireContext(), "Ingresa latitud y longitud", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("lat", latText)
                .putString("lon", lonText)
                .apply()

            Toast.makeText(requireContext(), "Ubicación guardada", Toast.LENGTH_SHORT).show()
        }
    }
}
