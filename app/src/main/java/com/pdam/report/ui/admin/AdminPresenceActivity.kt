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
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        setContent()

        // Menambahkan aksi ketika pengguna menarik untuk menyegarkan (swipe to refresh)
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

    // Mengatur konfigurasi RecyclerView
    private fun setupRecyclerView() {
        binding.rvPresence.apply {
            layoutManager = LinearLayoutManager(this@AdminPresenceActivity)
            setHasFixedSize(true)
            adapter = this@AdminPresenceActivity.adapter
        }
    }

    // Memuat data presensi dari Firebase Database
    private fun setContent() {
        val userRef = FirebaseDatabase.getInstance().getReference("listPresence")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presenceList = ArrayList<PresenceData>()

                // Iterasi melalui data presensi
                snapshot.children.forEach { presenceSnapshot ->
                    presenceSnapshot.children.forEach { presenceDataSnapshot ->
                        val presenceData = presenceDataSnapshot.getValue(PresenceData::class.java)
                        presenceData?.let { presenceList.add(it) }
                    }
                }

                if (presenceList.isNotEmpty()) {
                    // Menampilkan RecyclerView dan menyembunyikan pesan "Data Kosong"
                    setRecyclerViewVisibility(binding.tvEmptyItems, binding.rvPresence, false)

                    // Mengurutkan data presensi berdasarkan tanggal secara menurun
                    presenceList.sortByDescending { it.currentDate }

                    // Memperbarui data di adapter
                    adapter.updateData(presenceList)
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