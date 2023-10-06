package com.pdam.report

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.os.postDelayed
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.drawerlayout.widget.DrawerLayout
import com.pdam.report.data.UserPreference
import com.pdam.report.databinding.ActivityMainBinding
import com.pdam.report.ui.common.LoginActivity
import com.pdam.report.ui.officer.AddFirstDataActivity
import com.pdam.report.ui.officer.OfficerPresenceActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user")
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var toggle: ActionBarDrawerToggle
    private val userPreference: UserPreference by lazy {
        UserPreference.getInstance(dataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        supportActionBar?.hide()
        setupView()

        binding.buttonAdd.setOnClickListener {
            val moveIntent = Intent(this@MainActivity, AddFirstDataActivity::class.java)
            startActivity(moveIntent)
        }
    }

    private fun setupView(){
        lifecycleScope.launch {
            try {
                val user = userPreference.getUser().first()
                Toast.makeText(this@MainActivity, user.toString(), Toast.LENGTH_SHORT).show()
                if (user.isLogin) {
                    supportActionBar?.show()
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    setContentView(binding.root)
                    setupDrawerLayout()
                    setupNavigationMenu()
                } else {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDrawerLayout() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupNavigationMenu() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_report -> {
                    val moveIntent = Intent(this@MainActivity, MainActivity::class.java)
                    moveIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(moveIntent)
                }
                R.id.nav_presence -> {
                    val moveIntent = Intent(this@MainActivity, OfficerPresenceActivity::class.java)
                    startActivity(moveIntent)
                }
                R.id.nav_logout -> {
                    Toast.makeText(
                        applicationContext,
                        "Logged out",
                        Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        userPreference.logoutUser()
                    }
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}