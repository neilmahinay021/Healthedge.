package com.example.healthedge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.example.healthedge.databinding.ActivityRegisterTabbedBinding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterTabbedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterTabbedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = RegisterPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Patient Registration" else "Doctor Registration"
        }.attach()
    }
} 