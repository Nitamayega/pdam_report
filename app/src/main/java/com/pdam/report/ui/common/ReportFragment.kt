package com.pdam.report.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainAdapter
import com.pdam.report.R
import com.pdam.report.data.SambunganData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.FragmentReportBinding
import com.pdam.report.utils.setRecyclerViewVisibility
import com.pdam.report.utils.showLoading
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReportFragment : Fragment(R.layout.fragment_report) {
    private var _binding: FragmentReportBinding? = null
    private val args by lazy { requireArguments() }
    private val user by lazy { args.getParcelable<UserData>("user")!! }
    private val adapterPemasangan by lazy { MainAdapter(ArrayList(), 0, user) }
    private val adapterPemutusan by lazy { MainAdapter(ArrayList(), 1, user) }
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportBinding.bind(view)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (args.getInt(ARG_SECTION_NUMBER) == 1) {
                setContentPemasangan(user)
                binding.swipeRefreshLayout.setOnRefreshListener {
                    setContentPemasangan(user)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } else {
                setContentPemutusan(user)
                binding.swipeRefreshLayout.setOnRefreshListener {
                    setContentPemutusan(user)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // stop coroutine
        lifecycleScope.coroutineContext.cancel()
    }

    private fun setContentPemutusan(user: UserData) {
        showLoading(true, binding.progressBar)
        val listPemutusanRef = FirebaseDatabase.getInstance().getReference("listPemutusan")

        listPemutusanRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.hasChildren()) {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, false)
                    binding.rvCusts.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                    }

                    val customerList = snapshot.children.mapNotNull { customerSnapshot ->
                        customerSnapshot.getValue(SambunganData::class.java)
                    }

                    // Check if user.dailyTeam is 0
                    if (user.dailyTeam == 0) {
                        // Use all data without filtering
                        adapterPemutusan.updateData(customerList.sortedByDescending { it.currentDate })
                    } else {
                        // Filter item sesuai dengan user.dailyTeam
                        val filteredCustomerList = customerList.filter { customer ->
                            customer.dailyTeam == user.dailyTeam
                        }
                        adapterPemutusan.updateData(filteredCustomerList.sortedByDescending { it.currentDate })
                    }

                    binding.rvCusts.adapter = adapterPemutusan
                    showLoading(false, binding.progressBar)

                    if (binding.rvCusts.adapter?.itemCount == 0) {
                        setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                        showLoading(false, binding.progressBar)
                    }
                } else {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                    showLoading(false, binding.progressBar)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false, binding.progressBar)
            }
        })
    }

    private fun setContentPemasangan(user: UserData) {
        showLoading(true, binding.progressBar)
        val listPemasanganRef = FirebaseDatabase.getInstance().getReference("listPemasangan")

        listPemasanganRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.hasChildren()) {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, false)
                    binding.rvCusts.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                    }

                    val customerList = snapshot.children.mapNotNull { customerSnapshot ->
                        customerSnapshot.getValue(SambunganData::class.java)
                    }

                    // Check if user.dailyTeam is 0
                    if (user.dailyTeam == 0) {
                        // Use all data without filtering
                        adapterPemasangan.updateData(customerList.sortedByDescending { it.currentDate })
                    } else {
                        // Filter item sesuai dengan user.dailyTeam
                        val filteredCustomerList = customerList.filter { customer ->
                            customer.dailyTeam == user.dailyTeam
                        }
                        adapterPemasangan.updateData(filteredCustomerList.sortedByDescending { it.currentDate })
                    }

                    binding.rvCusts.adapter = adapterPemasangan
                    showLoading(false, binding.progressBar)

                    if (binding.rvCusts.adapter?.itemCount == 0) {
                        setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                        showLoading(false, binding.progressBar)
                    }


                } else {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                    showLoading(false, binding.progressBar)
                }


            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false, binding.progressBar)
            }
        })
    }


    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(index: Int, user: UserData?) = ReportFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SECTION_NUMBER, index)
                putParcelable("user", user)
            }
        }
    }
}