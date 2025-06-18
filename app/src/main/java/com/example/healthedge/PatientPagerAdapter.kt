package com.example.healthedge

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.healthedge.models.User

class PatientPagerAdapter(activity: FragmentActivity, private val patients: List<User>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = patients.size
    override fun createFragment(position: Int): Fragment = PatientDetailsFragment.newInstance(patients[position])
} 