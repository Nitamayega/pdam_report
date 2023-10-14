package com.pdam.report

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.data.DataCustomer
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityMainBinding
import com.pdam.report.ui.admin.AdminPresenceActivity
import com.pdam.report.ui.common.LoginActivity
import com.pdam.report.ui.officer.AddFirstDataActivity
import com.pdam.report.ui.officer.OfficerPresenceActivity
import com.pdam.report.utils.GeocoderHelper
import com.pdam.report.utils.PermissionHelper
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.setRecyclerViewVisibility
import com.pdam.report.utils.showToast
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val adapter by lazy { MainAdapter(ArrayList()) }
    private lateinit var toggle: ActionBarDrawerToggle

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private val fuse: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var latLng: LatLng? = null
    private val geocoderHelper = GeocoderHelper(this)
    private lateinit var locationRequest: LocationRequest

    private val userManager by lazy { UserManager(this) }
    private lateinit var user: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setupView()
        setupData()
        binding.swipeRefreshLayout.setOnRefreshListener {
            setContent()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupData() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
            setupNavigationHeader()
            setupNavigationMenu()
            setContent()

            if (user.team == 0) {
                binding.buttonAdd.visibility = View.GONE
            } else {
                binding.buttonAdd.visibility = View.VISIBLE
                binding.buttonAdd.setOnClickListener {
                    val moveIntent = Intent(this@MainActivity, AddFirstDataActivity::class.java)
                    startActivity(moveIntent)
                }
            }
        }
    }

    private fun setupView() {
        if (currentUser == null) {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            supportActionBar?.show()
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setContentView(binding.root)
            setupDrawerLayout()
        }

        // Check and request camera and location permissions
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this)
        }
        if (!PermissionHelper.hasLocationPermission(this)) {
            PermissionHelper.requestLocationPermission(this)
        }
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
                    val moveIntent = Intent(this@MainActivity, MainActivity::class.java)
                    moveIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(moveIntent)
                }
                R.id.nav_presence -> {
                    val currentDate = Date().time
                    val referenceDate = SimpleDateFormat("dd/MM/yyyy").parse("03/10/2023")?.time
                    val daysDifference = ((currentDate - referenceDate!!) / (1000L * 60 * 60 * 24) % 5).toInt()

                    val moveIntent = when {
                        user.team == 0 -> {
                            Intent(this@MainActivity, AdminPresenceActivity::class.java)
                        }
                        user.team == daysDifference -> {
                            Intent(this@MainActivity, OfficerPresenceActivity::class.java)
                        }
                        else -> {
                            showToast(this@MainActivity, R.string.permission_denied)
                            null
                        }
                    }

                    moveIntent?.let {
                        startActivity(it)
                    }
                }
                R.id.nav_logout -> {
                    Toast.makeText(
                        applicationContext,
                        "Logged out",
                        Toast.LENGTH_SHORT
                    ).show()
                    auth.signOut()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            true
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

        if (user.team == 0) {
            role.text = resources.getStringArray(R.array.roles)[1]
        } else {
            role.text = resources.getStringArray(R.array.roles)[0]
        }

        Glide.with(this@MainActivity)
            .load("https://ui-avatars.com/api/?name=$displayName&background=1C6996&color=fff")
            .placeholder(R.drawable.logo1)
            .optionalFitCenter()
            .into(photo)
    }

    private fun setContent() {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser?.uid ?: "")
        val listCustomerRef = userRef.child("listCustomer")

        listCustomerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, false)
                    binding.rvCusts.apply {
                        layoutManager = LinearLayoutManager(this@MainActivity)
                        setHasFixedSize(true)
                    }

                    val customerList = ArrayList<DataCustomer>()
                    for (customerSnapshot in snapshot.children) {
                        val customer = customerSnapshot.getValue(DataCustomer::class.java)
                        customer?.let {
                            customerList.add(it)
                        }
                    }
                    customerList.sortByDescending { it.currentDate }
                    adapter.updateData(customerList)
                    binding.rvCusts.adapter = adapter
                } else {
                    setRecyclerViewVisibility(binding.emptyView, binding.rvCusts, true)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event

            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
