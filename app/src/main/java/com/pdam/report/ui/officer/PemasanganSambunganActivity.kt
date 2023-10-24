package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.CustomerData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemasanganSambunganBinding
import com.pdam.report.utils.FullScreenImageDialogFragment
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.parsingNameImage
import com.pdam.report.utils.reduceFileImageInBackground
import com.pdam.report.utils.showDeleteConfirmationDialog
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import kotlinx.coroutines.launch
import java.io.File

class PemasanganSambunganActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPemasanganSambunganBinding.inflate(layoutInflater) }
    private var imageFile: File? = null

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val firebaseKey by lazy { intent.getStringExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY) }
    private val customerData by lazy { intent.getIntExtra(PemasanganKelayakanActivity.EXTRA_CUSTOMER_DATA, 0) }

    private val userManager by lazy { UserManager(this) }
    private lateinit var user: UserData

    private var imageNumber: Int = 0
    private var firstImageFile: File? = null
    private var secondImageFile: File? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
//            navigatePage(this@UpdateCustomerInstallationActivity, AddFirstDataActivity::class.java, true)
            val intent = Intent(this@PemasanganSambunganActivity, PemasanganKelayakanActivity::class.java)
            intent.putExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY, firebaseKey.toString())
            startActivity(intent)
            finish()
        }
    }

    private fun setUser() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadDataFromFirebase(firebaseKey.toString())

        setupButtons()
        setUser()
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener { imageNumber = 1; startTakePhoto() }
        binding.itemImage2.setOnClickListener { imageNumber = 2; startTakePhoto() }

        binding.btnSimpan.setOnClickListener { saveData() }
        binding.btnHapus.setOnClickListener {
            if (customerData == 1) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun deleteData() {
        val listCustomerRef = databaseReference.child("listCustomer")
        val customerRef = firebaseKey?.let { listCustomerRef.child(it) }

        customerRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Show a confirmation dialog for delete
                    showDeleteConfirmationDialog(customerRef, this@PemasanganSambunganActivity)
                } else {
                    showToast(this@PemasanganSambunganActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@PemasanganSambunganActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private fun clearData() {
        // Clear all input fields
        binding.edtNomorKl.text.clear()
        binding.edtMerk.text.clear()
        binding.edtDiameter.text.clear()
        binding.edtStand.text.clear()
        binding.edtNomorMeter.text.clear()
        binding.edtNomorSegel.text.clear()
        binding.edtKeterangan.text.clear()
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
        val nomorKL = binding.edtNomorKl.text.toString()
        val merkMeter = binding.edtMerk.text.toString()
        val diameterMeter = binding.edtDiameter.text.toString()
        val standMeter = binding.edtStand.text.toString()
        val nomorMeter = binding.edtNomorMeter.text.toString()
        val nomorSegel = binding.edtNomorSegel.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        // Validate input
        if (isInputValid(nomorKL, merkMeter, diameterMeter, standMeter, nomorMeter, nomorSegel, keterangan)) {
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            uploadImagesAndSaveData(user.username, currentDate, nomorKL, merkMeter, diameterMeter, standMeter, nomorMeter, nomorSegel, keterangan)
        } else {
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_data)
        }
    }

    private fun isInputValid(nomorKL: String, merk: String, diameter: String, stand: String, nomorMeter: String, nomorSegel: String, keterangan: String): Boolean {
        // Check if all required input is valid
        return nomorKL.isNotEmpty() && merk.isNotEmpty() && diameter.isNotEmpty() && stand.isNotEmpty() && nomorMeter.isNotEmpty() && nomorSegel.isNotEmpty() && keterangan.isNotEmpty() && imageFile != null
    }

    private fun uploadImagesAndSaveData(petugas: String, currentDate: Long, nomorKL: String, merk: String, diameter: String, stand: String, nomorMeter: String, nomorSegel: String, keterangan: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val dokumentasi3Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi3_konstruksi.jpg")
        val dokumentasi4Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi4_meter.jpg")

        lifecycleScope.launch {
            firstImageFile = firstImageFile?.reduceFileImageInBackground()
            secondImageFile = secondImageFile?.reduceFileImageInBackground()
        }

        // Upload image 3
        dokumentasi3Ref.putFile(Uri.fromFile(imageFile)).addOnSuccessListener {
            dokumentasi3Ref.downloadUrl.addOnSuccessListener { uri1 ->
                val dokumentasi3 = uri1.toString()

                dokumentasi4Ref.putFile(Uri.fromFile(secondImageFile)).addOnSuccessListener {
                    dokumentasi4Ref.downloadUrl.addOnSuccessListener { uri2 ->
                        val dokumentasi4 = uri2.toString()

                        // After successfully obtaining image URLs, save the data to Firebase
                        saveCustomerData(
                            petugas,
                            currentDate,
                            nomorKL,
                            merk,
                            diameter,
                            stand,
                            nomorMeter,
                            nomorSegel,
                            keterangan,
                            dokumentasi3,
                            dokumentasi4
                        )
                    }
                }
            }
        }
    }

    private fun saveCustomerData(petugas: String, currentDate: Long, nomorKL: String, merk: String, diameter: String, stand: String, nomorMeter: String, nomorSegel: String, keterangan: String, dokumentasi3: String, dokumentasi4: String) {
        val customerRef = databaseReference.child("listCustomer").child(firebaseKey.toString())

        val data = mapOf(
            "petugas" to petugas,
            "updateVerifDate" to currentDate,
            "nomorKL" to nomorKL,
            "merkMeter" to merk,
            "diameterMeter" to diameter,
            "standMeter" to stand,
            "nomorMeter" to nomorMeter,
            "nomorSegel" to nomorSegel,
            "keterangan2" to keterangan,
            "dokumentasi3" to dokumentasi3,
            "dokumentasi4" to dokumentasi4,
            "data" to 2
        )

        customerRef.updateChildren(data).addOnCompleteListener { task ->
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            if (task.isSuccessful) {
                showToast(this, R.string.save_success)
            } else {
                showToast(this, R.string.save_failed)
            }
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            finish()
        }
    }

    private fun loadDataFromFirebase(firebaseKey: String) {
        val customerRef = databaseReference.child("listCustomer").child(firebaseKey)

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(CustomerData::class.java)
                    if (dataCustomer != null) {
                        // Jika data pelanggan ditemukan, tampilkan datanya
                        displayCustomerData(dataCustomer)
                        if (dataCustomer.data != 1) {
                            displayAnotherData(dataCustomer)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan kesalahan jika mengakses data gagal
                showToast(this@PemasanganSambunganActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@PemasanganSambunganActivity,
                "com.pdam.report",
                file
            )
            currentPhotoPath = file.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launcherIntentCamera.launch(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // After successfully capturing an image, assign it to the appropriate file
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                if (imageNumber == 1) {
                    firstImageFile = file
                    binding.itemImage1.text = System.currentTimeMillis().toString() + "_dokumentasi3_konstruksi.jpg"
                } else if (imageNumber == 2) {
                    secondImageFile = file
                    binding.itemImage2.text = System.currentTimeMillis().toString() + "_dokumentasi4_meter.jpg"
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this@PemasanganSambunganActivity, PemasanganKelayakanActivity::class.java)
            intent.putExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY, firebaseKey.toString())
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayCustomerData(dataCustomer: CustomerData) {
        // Mengisi tampilan dengan data pelanggan yang ditemukan dari Firebase
        binding.edtPemasanganSambungan.apply {
            setText(dataCustomer.jenisPekerjaan)
            isEnabled = false
            isFocusable = false
        }

        binding.edtPw.apply {
            setText(dataCustomer.pw.toString())
            isEnabled = false
            isFocusable = false
        }

        binding.edtNamaPelanggan.apply {
            setText(dataCustomer.name)
            isEnabled = false
            isFocusable = false
        }

        binding.edtAlamatPelanggan.apply {
            setText(dataCustomer.address)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKeterangan.apply {
            setText(dataCustomer.keterangan1)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayAnotherData(dataCustomer: CustomerData) {
        binding.edtNomorKl.apply {
            setText(dataCustomer.nomorKL)
            isEnabled = false
            isFocusable = false
        }

        binding.updatedby.apply {
            text = "Update by " + dataCustomer.petugas + " at " + dataCustomer.updateInstallDate.toString()
            isEnabled = false
            isFocusable = false
            visibility = android.view.View.VISIBLE
        }

        binding.edtMerk.apply {
            setText(dataCustomer.merkMeter)
            isEnabled = false
            isFocusable = false
        }

        binding.edtDiameter.apply {
            setText(dataCustomer.diameterMeter)
            isEnabled = false
            isFocusable = false
        }

        binding.edtStand.apply {
            setText(dataCustomer.standMeter)
            isEnabled = false
            isFocusable = false
        }

        binding.edtNomorMeter.apply {
            setText(dataCustomer.nomorMeter)
            isEnabled = false
            isFocusable = false
        }

        binding.edtNomorSegel.apply {
            setText(dataCustomer.nomorSegel)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKeterangan.apply {
            setText(dataCustomer.keterangan2)
            isEnabled = false
            isFocusable = false
        }

        binding.itemImage1.apply {
            text = parsingNameImage(dataCustomer.dokumentasi3)
            setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi3), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.itemImage2.apply {
            text = parsingNameImage(dataCustomer.dokumentasi4)
            setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi4), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            if (dataCustomer.jenisPekerjaan == "Pemasangan kembali") {
                text = getString(R.string.finish)
                setOnClickListener {
                    navigatePage(this@PemasanganSambunganActivity, MainActivity::class.java, true)
                }
            } else {
                text = getString(R.string.next)
                setOnClickListener {
                    val intent = Intent(
                        this@PemasanganSambunganActivity,
                        PemasanganGPSActivity::class.java
                    )

                    intent.putExtra(
                        PemasanganGPSActivity.EXTRA_FIREBASE_KEY,
                        dataCustomer.firebaseKey
                    )
                    intent.putExtra(
                        PemasanganGPSActivity.EXTRA_CUSTOMER_DATA,
                        dataCustomer.data
                    )

                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}