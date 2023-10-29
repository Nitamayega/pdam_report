package com.pdam.report

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pdam.report.data.UserData
import com.pdam.report.ui.common.ReportFragment

class SectionPagerAdapter(activity: AppCompatActivity, private val user: UserData) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return ReportFragment.newInstance(position + 1, user)
    }
}
