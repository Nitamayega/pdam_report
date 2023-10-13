package com.pdam.report.ui.admin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.ActivityAdminPresenceBinding
import com.pdam.report.utils.setRecyclerViewVisibility

class AdminPresenceActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAdminPresenceBinding.inflate(layoutInflater) }
    private val adapter by lazy { AdminPresenceAdapter(ArrayList()) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(binding.root)
        setContent()
        binding.swipeRefreshLayout.setOnRefreshListener {
            setContent()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setContent() {
        val userRef = FirebaseDatabase.getInstance().getReference("listPresence")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    setRecyclerViewVisibility(binding.tvEmptyItems, binding.rvPresence, false)
                    binding.rvPresence.apply {
                        layoutManager = LinearLayoutManager(this@AdminPresenceActivity)
                        setHasFixedSize(true)
                    }

                    val presenceList = ArrayList<PresenceData>()
                    for (presenceSnapshot in snapshot.children) {
                        for (presenceDataSnapshot in presenceSnapshot.children) {
                            val presenceData = presenceDataSnapshot.getValue(PresenceData::class.java)
                            presenceData?.let {
                                presenceList.add(it)
                            }
                        }
                    }

                    presenceList.sortByDescending { it.currentDate }
                    adapter.updateData(presenceList)
                    binding.rvPresence.adapter = adapter
                } else {
                    setRecyclerViewVisibility(binding.tvEmptyItems, binding.rvPresence, true)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event
            }
        })
    }

}