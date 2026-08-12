package com.rumahsehat.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/** Pager assessment: halaman-0 = identitas, sisanya = soal (indeks soal = position - 1). */
class QuestionPagerAdapter(activity: FragmentActivity, private val count: Int) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = count

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) IdentityFragment.newInstance()
        else QuestionFragment.newInstance(position - 1)
    }
}