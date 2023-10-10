package com.pdam.report.ui.officer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.PresenceData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityOfficerPresenceBinding
import com.pdam.report.utils.GeocoderHelper
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.reduceFileImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfficerPresenceActivity : AppCompatActivity() {

    private var getFile: File? = null
    private var storageReference = FirebaseStorage.getInstance().reference
    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val fuse: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var latLng: LatLng? = null
    private val geocoderHelper = GeocoderHelper(this)
    private lateinit var locationRequest: LocationRequest

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private var isToastShown = false

    private val binding: ActivityOfficerPresenceBinding by lazy { ActivityOfficerPresenceBinding.inflate(layoutInflater) }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inisialisasi locationRequest di sini
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // Interval pembaruan lokasi dalam milidetik (10 detik)
            fastestInterval = 5000 // Interval tercepat pembaruan lokasi dalam milidetik (5 detik)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Prioritas akurasi tinggi
        }

        checkPermissions()
        setupButtons()
        getLocation()
    }

    private fun setupButtons() {
        binding.cameraButton.setOnClickListener { startTakePhoto() }
        binding.uploadButton.isEnabled = false
        binding.uploadButton.setOnClickListener { uploadImage() }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        if (hasLocationPermissions()) {
            fuse.removeLocationUpdates(locationCallback)
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateToMainActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        navigateToMainActivity()
        super.onBackPressed()
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@OfficerPresenceActivity,
                "com.pdam.report",
                file
            )
            currentPhotoPath = file.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launcherIntentCamera.launch(intent)
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                getFile = file
                binding.previewImageView.setImageBitmap(BitmapFactory.decodeFile(file.path))
            }
        }
    }

    private fun uploadImage() {
        if (getFile != null) {
            val uid = currentUser?.uid

            if (uid != null) {
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userData = snapshot.getValue(UserData::class.java)
                            if (userData != null) {
                                val username = userData.username

                                showLoading(true)

                                //Compress size photo
                                val compressedFile = reduceFileImage(getFile!!)

                                val photoRef = storageReference.child("images/presence/${System.currentTimeMillis()}.jpg")
                                photoRef.putFile(Uri.fromFile(compressedFile)).addOnSuccessListener { uploadTask ->
                                    uploadTask.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                                        showLoading(false)
                                        val data = PresenceData(
                                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                            username ?: "", // Menggunakan username dari Realtime Database
                                            latLng?.let { geocoderHelper.getAddressFromLatLng(it).toString() } ?: "",
                                            downloadUri.toString(),
                                        )

                                        databaseReference.child("listPresence")
                                            .child(uid).push().setValue(data)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    showToast(getString(R.string.upload_success))
                                                    navigateToMainActivity()
                                                } else {
                                                    showToast(getString(R.string.upload_failed))
                                                }
                                            }
                                    }.addOnFailureListener {
                                        showLoading(false)
                                        showToast(it.message.toString())
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showLoading(false)
                        showToast("Error: ${error.message}")
                    }
                })
            } else {
                showToast("User not authenticated.")
            }
        } else {
            showToast(getString(R.string.select_image))
        }
    }

    private fun checkPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this@OfficerPresenceActivity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                latLng = LatLng(location.latitude, location.longitude)
                Log.d("LatLong", latLng.toString())
                if (latLng != null) {
                    if (!isToastShown) {
                        showToast(getString(R.string.location_found))
                        binding.uploadButton.isEnabled = true
                        isToastShown = true
                    }
                } else {
                    showToast(getString(R.string.location_not_found))
                }
            }
        }
    }

    private fun getLocation() {
        if (latLng != null){
            showToast(getString(R.string.initialize_location))
        }
        if (hasLocationPermissions()) {
            try {
                fuse.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                showToast(getString(R.string.permission_denied))
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this@OfficerPresenceActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this@OfficerPresenceActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this@OfficerPresenceActivity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    getLocation()
                } else {
                    showToast(getString(R.string.permission_denied))
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cameraButton.isEnabled = !isLoading
        binding.uploadButton.isEnabled = !isLoading
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}