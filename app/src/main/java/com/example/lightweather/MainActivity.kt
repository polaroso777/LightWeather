package com.example.lightweather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lightweather.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TodayFragment())
                .commit()
        }

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
    }
}
