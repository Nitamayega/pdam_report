package com.pdam.report.utils

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener

fun showLoading(isLoading: Boolean, view: View, firstButton: Button? = null, secondButton: Button? = null) {
    view.visibility = if (isLoading) View.VISIBLE else View.GONE
    firstButton?.isEnabled = !isLoading
    secondButton?.isEnabled = !isLoading
}

fun setRecyclerViewVisibility(emptyView: View, recyclerView: RecyclerView, emptyViewVisible: Boolean) {
    emptyView.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
    recyclerView.visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
}

fun showToast(context: Context, resId: Int) {
    val message = context.getString(resId)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun navigatePage(context: Context, destination: Class<*>, clearTask: Boolean = false) {
    val intent = Intent(context, destination)
    if (clearTask) {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
