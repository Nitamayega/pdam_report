package com.pdam.report.ui.common

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.UserData
import com.pdam.report.data.UserPreference
import com.pdam.report.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupView()
        setupAction()
    }

    private fun setupAction() {
        binding.buttonLogin.setOnClickListener {
            val uname = binding.edtUsername.text.toString()
            val isUnameEmpty = uname.isEmpty()
            val password = binding.passwordEditText.text.toString()
            val isPasswordEmpty = password.isEmpty()

            binding.edtUsername.error = if (isUnameEmpty) getString(R.string.empty_field) else null
            binding.passwordEditText.error = if (isPasswordEmpty) getString(R.string.empty_field) else null

            if (!isUnameEmpty && !isPasswordEmpty) {
                showLoading(true)
                val email = "$uname@pdam.com"
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    showLoading(false)

                    if (task.isSuccessful) {
                        onLoginSuccess()
                    } else {
                        onLoginFailed()
                    }
                }
            }
        }
    }

    private fun setupView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onLoginSuccess() {
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun onLoginFailed() {
        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show()
    }
}