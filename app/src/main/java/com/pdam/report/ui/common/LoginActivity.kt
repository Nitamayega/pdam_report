package com.pdam.report.ui.common

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.databinding.ActivityLoginBinding
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast

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
            val password = binding.passwordEditText.text.toString()

            binding.edtUsername.error =
                if (uname.isEmpty()) getString(R.string.empty_field) else null
            binding.passwordEditText.error =
                if (password.isEmpty()) getString(R.string.empty_field) else null

            if (uname.isNotEmpty() && password.isNotEmpty()) {
                showLoading(true, binding.progressBar, binding.buttonLogin)
                val email = "$uname@pdam.com"
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        showLoading(false, binding.progressBar, binding.buttonLogin)

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

    private fun onLoginSuccess() {
        showToast(this, R.string.login_success)
        navigatePage(this, MainActivity::class.java, clearTask = true)
    }

    private fun onLoginFailed() {
        showToast(this, R.string.login_failed)
    }
}