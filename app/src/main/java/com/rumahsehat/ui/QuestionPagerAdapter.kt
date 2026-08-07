package com.rumahsehat.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class QuestionPagerAdapter(activity: FragmentActivity, private val count: Int) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = count

    override fun createFragment(position: Int): Fragment {
        return QuestionFragment.newInstance(position)
    }
}
