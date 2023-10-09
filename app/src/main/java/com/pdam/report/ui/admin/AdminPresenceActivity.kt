package com.pdam.report.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.data.DataCustomer
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.ActivityAdminPresenceBinding
import java.util.ArrayList

class AdminPresenceActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAdminPresenceBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setContentView(binding.root)
        setContent()
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
                    binding.rvPresence.apply {
                        visibility = View.VISIBLE
                        layoutManager = LinearLayoutManager(this@AdminPresenceActivity)
                        setHasFixedSize(true)
                    }

                    val presenceList = ArrayList<PresenceData>()
                    for (presenceSnapshot in snapshot.children) {
                        for (presenceDataSnapshot in presenceSnapshot.children) {
                            // Mengambil data dari setiap item dalam listPresence
                            val presenceData = presenceDataSnapshot.getValue(PresenceData::class.java)
                            presenceData?.let {
                                presenceList.add(it)
                            }
                        }
                    }

                    presenceList.sortByDescending { it.currentDate }
                    val adapter = AdminPresenceAdapter(presenceList)
                    binding.rvPresence.adapter = adapter
                } else {
                    binding.rvPresence.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event
            }
        })
    }

}