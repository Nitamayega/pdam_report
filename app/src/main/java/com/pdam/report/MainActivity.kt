package com.pdam.report

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import com.pdam.report.utils.UserManager

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var toggle: ActionBarDrawerToggle

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private val userManager by lazy { UserManager(this) }
    private lateinit var user: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        supportActionBar?.hide()
        setupView()
        setupData()

        binding.buttonAdd.setOnClickListener {
            val moveIntent = Intent(this@MainActivity, AddFirstDataActivity::class.java)
            startActivity(moveIntent)
        }

    }
    private fun setupData() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
            setupNavigationHeader()
            setupNavigationMenu()
            setContent()
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
                    val moveIntent = if (user.team == 0) {
                        Intent(this@MainActivity, AdminPresenceActivity::class.java)
                    } else {
                        Intent(this@MainActivity, OfficerPresenceActivity::class.java)
                    }
                    startActivity(moveIntent)
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
                    binding.emptyView.visibility = View.GONE
                    binding.rvCusts.apply {
                        visibility = View.VISIBLE
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
                    customerList.sortByDescending { it.createdAt }
//                    val adapter = MainAdapter(customerList)
//                    binding.rvCusts.adapter = adapter
                } else {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.rvCusts.visibility = View.GONE
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
