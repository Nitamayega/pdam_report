package com.pdam.report.utils

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

suspend fun getInitialDate(): String? {
    val database = Firebase.database.reference.child("initialDate")
    return try {
        val dataSnapshot = database.get().await()
        dataSnapshot.getValue(String::class.java)
    } catch (e: Exception) {
        null
    }
}
