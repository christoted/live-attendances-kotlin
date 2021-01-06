package com.example.liveattendances

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.liveattendances.databinding.ActivityMainBinding
import com.example.liveattendances.views.fragment.AttendanceFragment
import com.example.liveattendances.views.fragment.HistoryFragment
import com.example.liveattendances.views.fragment.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = HistoryFragment()
        setCurrentFragment(fragment)

        binding.btmNavigationMain.setOnNavigationItemSelectedListener {item ->
            when(item.itemId) {
                R.id.action_history -> {
                    val fragment = HistoryFragment()
                    setCurrentFragment(fragment)
                }

                R.id.action_attendance -> {
                    val fragment = AttendanceFragment()
                    setCurrentFragment(fragment)
                }

                R.id.action_profile -> {
                    val fragment = ProfileFragment()
                    setCurrentFragment(fragment)
                }
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frame_main, fragment)
            addToBackStack(null)
            commit()
        }
}