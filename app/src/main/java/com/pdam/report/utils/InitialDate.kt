package com.pdam.report.utils

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

// Fungsi untuk mengambil tanggal awal dari database Firebase
suspend fun getInitialDate(): String? {
    // Mendapatkan referensi database Firebase
    val database = Firebase.database.reference.child("initialDate")

    return try {
        // Mengambil data dari database
        val dataSnapshot = database.get().await()

        // Mengambil nilai dari dataSnapshot jika tidak null
        dataSnapshot.getValue(String::class.java)
    } catch (e: Exception) {
        // Menangani kesalahan dengan mengembalikan null
        null
    }
}
