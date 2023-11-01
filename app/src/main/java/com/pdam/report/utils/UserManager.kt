package com.pdam.report.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.data.UserData

class UserManager {

    // Firebase Authentication instance
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    // Firebase Database instance
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var user: UserData

    // Mendapatkan data pengguna saat ini
    fun getUser(): UserData {
        return user
    }

    // Mengambil data pengguna dari Firebase Database
    fun fetchUserAndSetupData(onDataFetched: () -> Unit) {
        val userRef = databaseReference.child("users").child(currentUser?.uid ?: "")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(UserData::class.java)!!
                onDataFetched()
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan error jika terjadi kesalahan saat pengambilan data
            }
        })
    }
}