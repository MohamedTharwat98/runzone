package com.example.runzone

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.runzone.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val homeFragment = HomeFragment()
        replaceFragment(homeFragment)

        binding.bottomNavigationView.setOnItemSelectedListener {item ->
            run {
                when (item.itemId) {
                    R.id.home_tab -> {
                        val homeFragment = HomeFragment()
                        replaceFragment(homeFragment)
                    }
                    R.id.sessionsList_tab -> {
                        val sessionsList = SessionsListFragment()
                        replaceFragment(sessionsList)
                    }
                    R.id.settings_tab -> {
                        val settingsFragment = SettingsFragment()
                        replaceFragment(settingsFragment)
                    }

                    else -> {}

                }
            }
            return@setOnItemSelectedListener true
        }

    }

    fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

}