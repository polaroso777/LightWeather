package com.example.lightweather

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etLat = view.findViewById<EditText>(R.id.etLat)
        val etLon = view.findViewById<EditText>(R.id.etLon)
        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            Toast.makeText(requireContext(),
                "Guardado: ${etLat.text}, ${etLon.text}", Toast.LENGTH_SHORT).show()
        }
    }
}
