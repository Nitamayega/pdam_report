package com.pdam.report

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
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
import com.pdam.report.ui.officer.PemutusanActivity
import com.pdam.report.utils.PermissionHelper
import com.pdam.report.utils.PermissionHelper.checkAndRequestPermissions
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.getInitialDate
import com.pdam.report.utils.getNetworkTime
import com.pdam.report.utils.milisToDate
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showDialogDenied
import com.pdam.report.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var toggle: ActionBarDrawerToggle

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

    private var index: MutableLiveData<Int> = MutableLiveData(0)


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))

        // Add the onPageChangeListener to the ViewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                index.value = if (position == 0) 0 else 1
            }
        })

        index.observe(this) {
            if (it == 0) {
                binding.fabAdd.setOnClickListener {
                    intent = Intent(this, PemasanganKelayakanActivity::class.java)
                    intent.putExtra(PemasanganKelayakanActivity.EXTRA_USER_DATA, user)
                    startActivity(intent)
                }
            } else if (it == 1) {
                binding.fabAdd.setOnClickListener {
                    navigatePage(this, PemutusanActivity::class.java)
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

                // Jika admin maka FAB hilang
                fabAdd.visibility = if (user.team == 0) GONE else VISIBLE
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

        checkAndRequestPermissions(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults)
        when (requestCode) {
            30 -> {
                // Handle camera and location permission result
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // Izin kamera atau lokasi ditolak, mungkin ingin menampilkan penjelasan kepada pengguna
                        showDialogDenied(this)
                        return
                    }
                }
                // Izin kamera dan lokasi diizinkan, lanjutkan dengan tindakan yang diinginkan
            }
            PermissionHelper.REQUEST_CAMERA_PERMISSION -> {
                // Handle camera permission result
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // Izin kamera ditolak, mungkin ingin menampilkan penjelasan kepada pengguna
                        showDialogDenied(this)
                        return
                    }
                }
                // Izin kamera diizinkan, lanjutkan dengan tindakan yang diinginkan
            }
            PermissionHelper.REQUEST_LOCATION_PERMISSION -> {
                // Handle location permission result
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        showDialogDenied(this)
                        return
                    }
                }
                // Izin lokasi diizinkan, lanjutkan dengan tindakan yang diinginkan
            }
        }
    }

    private fun setupDrawerLayout() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SimpleDateFormat")
    private fun setupNavigationMenu() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_report -> {
                    binding.drawerLayout.closeDrawers()
                }

                R.id.nav_presence -> {
                    GlobalScope.launch(Dispatchers.Default) {
                        val initialDate = async { getInitialDate() }
                        val currentDate = async { getNetworkTime() }

                        // Tunggu hingga initialDate dan currentDate tersedia.
                        val initialDateValue = initialDate.await()
                        val currentDateValue = currentDate.await()
                        val lastPresence = milisToDate(user.lastPresence)

                        val referenceDate = SimpleDateFormat("dd-MM-yyyy").parse(initialDateValue!!)?.time
                        var daysDifference = ((currentDateValue - referenceDate!!) / (1000L * 60 * 60 * 24) % 5).toInt()
                        if (daysDifference == 0) { daysDifference = 5 }

                        // convert current time to int with format 24 hours
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = currentDateValue
                        val currentTime = calendar.get(Calendar.HOUR_OF_DAY)

                        Log.d("MainActivity", "setupNavigationMenu Network:${milisToDateTime(currentDateValue)}, Local:${milisToDateTime(System.currentTimeMillis())}")

                        if (currentDateValue.toInt() != 0) {
                            if (lastPresence == milisToDate(currentDateValue)) {
                                withContext(Dispatchers.Main) {
                                    showToast(this@MainActivity, R.string.presence_already_done)
                                }
                                return@launch
                            } else {
                                val moveIntent = when {
                                    user.team == 0 -> Intent(this@MainActivity, AdminPresenceActivity::class.java)
    //                                user.team == daysDifference && currentTime in 19..23 -> Intent(this@MainActivity, OfficerPresenceActivity::class.java)
                                    else -> {
                                        withContext(Dispatchers.Main) {
    //                                        showToast(this@MainActivity, R.string.presence_denied)
                                            navigatePage(this@MainActivity, OfficerPresenceActivity::class.java)
                                        }
                                        return@launch
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    startActivity(moveIntent)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast(this@MainActivity, R.string.unavailable_network)
                            }
                        }
                    }
                    return@setNavigationItemSelectedListener false
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


    companion object {
        private val TAB_TITLES = arrayOf(
            R.string.pemasangan, R.string.pemutusan
        )
    }
}
