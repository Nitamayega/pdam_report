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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")
class LoginActivity : AppCompatActivity() {

    private val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val databaseReference by lazy { firebaseDatabase.reference.child("users/officer") }
    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val userPreference by lazy { UserPreference.getInstance(dataStore) }

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
                loginUser(uname, password)
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        databaseReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(UserData::class.java)

                        if (user != null && TextUtils.equals(user.password, password)) {
                            Toast.makeText(
                                this@LoginActivity,
                                R.string.login_success,
                                Toast.LENGTH_SHORT
                            ).show()

                            lifecycleScope.launch {
                                userPreference.loginUser(user.username, user.id, user.team)
                            }

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                            showLoading(false)
                            return
                        }
                    }
                }
                showLoading(false)
                Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
}