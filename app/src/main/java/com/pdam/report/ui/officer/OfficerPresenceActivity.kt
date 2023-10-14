package com.pdam.report.ui.officer

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
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
import com.pdam.report.utils.PermissionHelper
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.reduceFileImage
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import java.io.File

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        locationRequest = createLocationRequest()
        checkPermissions()
        setupButtons()
        checkLocationSettings()
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun setupButtons() {
        binding.cameraButton.setOnClickListener {
            if (PermissionHelper.hasCameraPermission(this@OfficerPresenceActivity)) {
                startTakePhoto()
            } else {
                PermissionHelper.requestCameraPermission(this@OfficerPresenceActivity)
            }
        }
        binding.uploadButton.isEnabled = false
        binding.uploadButton.setOnClickListener { uploadImage() }
    }

    private fun navigateToMainActivity() {
        if (PermissionHelper.hasLocationPermission(this@OfficerPresenceActivity)) { fuse.removeLocationUpdates(locationCallback) }
        navigatePage(this, MainActivity::class.java, true)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateToMainActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getLocation()
            } else if (resultCode == RESULT_CANCELED) {
                showToast(this, R.string.enable_location)
            }
        }
    }

    @Deprecated("Deprecated in Java")
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
                val userRef = databaseReference.child("users").child(uid)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userData = snapshot.getValue(UserData::class.java)
                            if (userData != null) {
                                val username = userData.username

                                showLoading(true, binding.progressBar, binding.cameraButton, binding.uploadButton)

                                val compressedFile = reduceFileImage(getFile!!)

                                val photoRef = storageReference.child("images/presence/${System.currentTimeMillis()}.jpg")
                                photoRef.putFile(Uri.fromFile(compressedFile)).addOnSuccessListener { uploadTask ->
                                    uploadTask.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                                        showLoading(false, binding.progressBar, binding.cameraButton, binding.uploadButton)
                                        val data = PresenceData(
                                            System.currentTimeMillis(),
                                            username,
                                            latLng?.let { geocoderHelper.getAddressFromLatLng(it).toString() } ?: "",
                                            downloadUri.toString(),
                                        )

                                        databaseReference.child("listPresence")
                                            .child(uid).push().setValue(data)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    showToast(this@OfficerPresenceActivity, R.string.upload_success)
                                                    navigateToMainActivity()
                                                } else {
                                                    showToast(this@OfficerPresenceActivity, R.string.upload_failed)
                                                }
                                            }
                                    }.addOnFailureListener {
                                        showLoading(false, binding.progressBar, binding.cameraButton, binding.uploadButton)
                                        showToast(this@OfficerPresenceActivity, it.message.toString().toInt())
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showLoading(false, binding.progressBar, binding.cameraButton, binding.uploadButton)
                        showToast(this@OfficerPresenceActivity, "Error: ${error.message}".toInt())
                    }
                })
            } else {
                showToast(this@OfficerPresenceActivity, R.string.invalid_auth)
            }
        } else {
            showToast(this@OfficerPresenceActivity, R.string.select_image)
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

    @Suppress("DEPRECATION")
    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            }
        }
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                latLng = LatLng(location.latitude, location.longitude)
                if (latLng != null && !isToastShown) {
                    showToast(this@OfficerPresenceActivity, R.string.location_found)
                    binding.uploadButton.isEnabled = true
                    isToastShown = true
                } else if (latLng == null) {
                    showToast(this@OfficerPresenceActivity, R.string.location_not_found)
                }
            }
        }
    }

    private fun getLocation() {
        if (latLng != null && !isToastShown) {
            showToast(this@OfficerPresenceActivity, R.string.initialize_location)
        }
        if (PermissionHelper.hasLocationPermission(this)) {
            try {
                fuse.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                showToast(this@OfficerPresenceActivity, R.string.permission_denied)
            }
        } else {
            PermissionHelper.requestLocationPermission(this@OfficerPresenceActivity)
        }
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
                    showToast(this@OfficerPresenceActivity, R.string.must_allow_permission)
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CHECK_SETTINGS = 123
    }
}