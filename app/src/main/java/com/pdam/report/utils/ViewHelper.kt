package com.pdam.report.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.pdam.report.R

fun showLoading(
    isLoading: Boolean,
    view: View,
    firstButton: Button? = null,
    secondButton: Button? = null,
) {
    view.visibility = if (isLoading) View.VISIBLE else View.GONE
    firstButton?.isEnabled = !isLoading
    secondButton?.isEnabled = !isLoading
}

fun setRecyclerViewVisibility(
    emptyView: View,
    recyclerView: RecyclerView,
    emptyViewVisible: Boolean,
) {
    emptyView.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
    recyclerView.visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
}

fun showToast(context: Context, resId: Int) {
    val message = context.getString(resId)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun showDeleteConfirmationDialog(customerRef: DatabaseReference, context: Context) {
    AlertDialog.Builder(context).apply {
        setTitle(R.string.delete_data)
        setMessage(R.string.delete_confirmation)
        setPositiveButton(R.string.delete) { _, _ ->
            // Confirm and proceed with deletion
            customerRef.removeValue()
                .addOnSuccessListener {
                    showToast(context, R.string.delete_success)
                    (context as Activity).finish()
                }
                .addOnFailureListener { error ->
                    showToast(context, "${R.string.delete_failed}: ${error.message}".toInt())
                }
        }
        setNegativeButton(R.string.cancel, null)
    }.create().show()
}

fun showDialogDenied(context: Context) {
    val dialog = AlertDialog.Builder(context)
    dialog.setTitle("Aplikasi perlu izin!")
    dialog.setMessage(context.getString(R.string.must_allow_permission))
    dialog.setPositiveButton("Pengaturan") { _, _ ->
        intentSetting(context)
    }

    dialog.setNegativeButton("Keluar") { _, _ ->
        (context as Activity).finish()
    }

    dialog.setCancelable(false)
    dialog.show()
}

fun navigatePage(context: Context, destination: Class<*>, clearTask: Boolean = false) {
    val intent = Intent(context, destination)
    if (clearTask) {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
