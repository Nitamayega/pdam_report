package com.pdam.report.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainAdapter
import com.pdam.report.R
import com.pdam.report.data.CustomerData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.FragmentReportBinding
import com.pdam.report.utils.setRecyclerViewVisibility

class ReportFragment : Fragment(R.layout.fragment_report) {
    private var _binding: FragmentReportBinding? = null
    private val adapter by lazy { MainAdapter(ArrayList()) }
    private lateinit var user: UserData
    private val binding get() = _binding!!

    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportBinding.bind(view)

        val args = requireArguments()
        user = args.getParcelable("user")!!

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setContentPemutusan(user: UserData) {
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
                        customerSnapshot.getValue(CustomerData::class.java)
                    }

                    // Check if user.dailyTeam is 0
                    if (user.dailyTeam == 0) {
                        // Use all data without filtering
                        adapter.updateData(customerList.sortedByDescending { it.currentDate })
                    } else {
                        // Filter item sesuai dengan user.dailyTeam
                        val filteredCustomerList = customerList.filter { customer ->
                            customer.dailyTeam == user.dailyTeam
                        }
                        adapter.updateData(filteredCustomerList.sortedByDescending { it.currentDate })
                    }

                    binding.rvCusts.adapter = adapter

                    if (binding.rvCusts.adapter?.itemCount == 0) {
                        setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                    }
                } else {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event
            }
        })
    }

    private fun setContentPemasangan(user: UserData) {
        val listPemasanganRef = FirebaseDatabase.getInstance().getReference("listCustomer")

        listPemasanganRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.hasChildren()) {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, false)
                    binding.rvCusts.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                    }

                    val customerList = snapshot.children.mapNotNull { customerSnapshot ->
                        customerSnapshot.getValue(CustomerData::class.java)
                    }

                    // Check if user.dailyTeam is 0
                    if (user.dailyTeam == 0) {
                        // Use all data without filtering
                        adapter.updateData(customerList.sortedByDescending { it.currentDate })
                    } else {
                        // Filter item sesuai dengan user.dailyTeam
                        val filteredCustomerList = customerList.filter { customer ->
                            customer.dailyTeam == user.dailyTeam
                        }
                        adapter.updateData(filteredCustomerList.sortedByDescending { it.currentDate })
                    }

                    binding.rvCusts.adapter = adapter

                    if (binding.rvCusts.adapter?.itemCount == 0) {
                        setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                    }


                } else {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                }


            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event
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