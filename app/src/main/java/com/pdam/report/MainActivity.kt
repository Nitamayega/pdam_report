package com.pdam.report

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityMainBinding
import com.pdam.report.ui.admin.AdminPresenceActivity
import com.pdam.report.ui.common.LoginActivity
import com.pdam.report.ui.officer.OfficerPresenceActivity
import com.pdam.report.ui.officer.PemasanganKelayakanActivity
import com.pdam.report.utils.PermissionHelper
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.getCurrentTimeStamp
import com.pdam.report.utils.getInitialDate
import com.pdam.report.utils.milisToDate
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showToast
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var toggle: ActionBarDrawerToggle

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

    private var index: MutableLiveData<Int> = MutableLiveData(0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Add the onPageChangeListener to the ViewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    index.value = 0
                } else {
                    index.value = 1
                }
            }
        })

        index.observe(this) {
            Log.d("MainActivity", "onCreate: $it")
            if (it == 0) {
                binding.fabAdd.setOnClickListener {
                    navigatePage(this, PemasanganKelayakanActivity::class.java)
                }
            } else {
                binding.fabAdd.setOnClickListener {
//                    navigatePage(this, PemasanganKelayakanActivity::class.java)
                    Toast.makeText(this, "Di Fragment Pemutusan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setupView()
        setupData()
    }


    private fun setupData() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
            setupNavigationHeader()
            setupNavigationMenu()
            val sectionPagerAdapter = SectionPagerAdapter(this, user)
            binding.apply {
                viewPager.adapter = sectionPagerAdapter
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = getString(TAB_TITLES[position])
                }.attach()
                supportActionBar?.elevation = 0f
            }
        }
    }



    private fun setupView() {
        if (currentUser == null) {
            navigatePage(this, LoginActivity::class.java)
            finish()
            return
        } else {
            supportActionBar?.show()
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setContentView(binding.root)
            setupDrawerLayout()
        }

        checkAndRequestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
    }

    private fun setupDrawerLayout() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupNavigationMenu() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_report -> {
                    binding.drawerLayout.closeDrawers()
                }

                R.id.nav_presence -> {

                    val initialDate = runBlocking { getInitialDate() }
                    val currentDate = SimpleDateFormat("dd-MM-yyyy").parse(getCurrentTimeStamp())?.time
                    val referenceDate = SimpleDateFormat("dd-MM-yyyy").parse(initialDate.toString())?.time
                    var daysDifference = ((currentDate!! - referenceDate!!) / (1000L * 60 * 60 * 24) % 5).toInt()
                    if (daysDifference == 0) { daysDifference = 5 }

//                    val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) && currentTime in 19..23

                    if (user.lastPresence == milisToDate(currentDate)) {
                        showToast(this, R.string.presence_already_done)
                        return@setNavigationItemSelectedListener false
                    }

                    val moveIntent = when {
                        user.team == 0 -> Intent(this@MainActivity, AdminPresenceActivity::class.java)
                        user.team == daysDifference -> Intent(this@MainActivity, OfficerPresenceActivity::class.java)
                        else -> {
                            showToast(this@MainActivity, R.string.presence_denied)
                            null
                        }
                    }

                    moveIntent?.let {
                        startActivity(it)
                    }
                }

                R.id.nav_logout -> {
                    showToast(applicationContext, R.string.logged_out)
                    auth.signOut()
                    navigatePage(this, LoginActivity::class.java, true)
                    finish()
                }
            }
            false
        }
    }

    private fun setupNavigationHeader() {
        val navView: NavigationView = binding.navView
        val header = navView.getHeaderView(0)
        val role = header.findViewById<TextView>(R.id.role)
        val uname = header.findViewById<TextView>(R.id.username)
        val photo = header.findViewById<ImageView>(R.id.profile_image)

        val displayName = user.username
        uname.text = displayName
        role.text = resources.getStringArray(R.array.roles)[if (user.team == 0) 1 else 0]

        Glide.with(this@MainActivity)
            .load("https://ui-avatars.com/api/?name=$displayName&background=1C6996&color=fff")
            .placeholder(R.drawable.logo1)
            .optionalFitCenter()
            .into(photo)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this)
        }
        if (!PermissionHelper.hasLocationPermission(this)) {
            PermissionHelper.requestLocationPermission(this)
        }
    }

    companion object {
        private val TAB_TITLES = arrayOf(
            R.string.pemasangan, R.string.pemutusan
        )
    }
}
