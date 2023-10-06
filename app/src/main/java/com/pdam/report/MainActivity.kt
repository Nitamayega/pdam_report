package com.pdam.report

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import com.pdam.report.databinding.ActivityMainBinding
import com.pdam.report.ui.common.LoginActivity
import com.pdam.report.ui.officer.AddFirstDataActivity
import com.pdam.report.ui.officer.OfficerPresenceActivity
import com.pdam.report.ui.officer.UpdateCustomerInstallationActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupDrawerLayout()
        setupNavigationMenu()

        binding.buttonAdd.setOnClickListener {
            val moveIntent = Intent(this@MainActivity, UpdateCustomerInstallationActivity::class.java)
            startActivity(moveIntent)
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