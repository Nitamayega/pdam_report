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
import com.pdam.report.databinding.ActivityPemasanganGpsBinding
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

class PemasanganGPSActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPemasanganGpsBinding.inflate(layoutInflater) }

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val firebaseKey by lazy { intent.getStringExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY) }
    private val customerData by lazy { intent.getIntExtra(PemasanganKelayakanActivity.EXTRA_CUSTOMER_DATA, 0) }

    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

    private var imageNumber: Int = 0
    private var firstImageFile: File? = null
    private var secondImageFile: File? = null
    private var thirdImageFile: File? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val intent = Intent(this@PemasanganGPSActivity, PemasanganSambunganActivity::class.java)
            intent.putExtra(PemasanganSambunganActivity.EXTRA_FIREBASE_KEY, firebaseKey)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firebaseKey?.let {
            loadDataFromFirebase(it)
        }

        setupButtons()
        setUser()
    }

    private fun setUser() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
        }
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener { imageNumber = 1; startTakePhoto() }
        binding.itemImage2.setOnClickListener { imageNumber = 2; startTakePhoto() }
        binding.itemImage3.setOnClickListener { imageNumber = 3; startTakePhoto() }
        binding.btnSimpan.setOnClickListener { saveData() }
        binding.btnHapus.setOnClickListener {
            if (customerData == 2) {
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
                    showDeleteConfirmationDialog(customerRef, this@PemasanganGPSActivity)
                } else {
                    showToast(this@PemasanganGPSActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@PemasanganGPSActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private fun clearData() {
        // Clear all input fields
        binding.edtX.text.clear()
        binding.edtY.text.clear()
        binding.edtZ.text.clear()
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
        val koorX = binding.edtX.text.toString()
        val koorY = binding.edtY.text.toString()
        val koorZ = binding.edtZ.text.toString()

        // Validate input
        if (isInputValid(koorX, koorY, koorZ)) {
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            uploadImagesAndSaveData(user.username, currentDate, koorX, koorY, koorZ)
        } else {
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_dataImage)
        }
    }

    private fun isInputValid(koorX: String, koorY: String, koorZ: String): Boolean {
        // Check if all required input is valid
        return koorX.isNotEmpty() && koorY.isNotEmpty() && koorZ.isNotEmpty() && (firstImageFile != null) && (secondImageFile != null) && (thirdImageFile != null)
    }

    private fun uploadImagesAndSaveData(petugas: String, currentDate: Long, koorX: String, koorY: String, koorZ: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val dokumentasi3Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi3_konstruksi.jpg")
        val dokumentasi4Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi4_meter.jpg")
        val dokumentasi5Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi5_perspektif.jpg")


        lifecycleScope.launch {
            firstImageFile = firstImageFile?.reduceFileImageInBackground()
            secondImageFile = secondImageFile?.reduceFileImageInBackground()
            thirdImageFile = thirdImageFile?.reduceFileImageInBackground()
        }

        dokumentasi3Ref.putFile(Uri.fromFile(firstImageFile)).addOnSuccessListener {
            dokumentasi3Ref.downloadUrl.addOnSuccessListener { uri1 ->
                val dokumentasi3 = uri1.toString()

                dokumentasi4Ref.putFile(Uri.fromFile(secondImageFile)).addOnSuccessListener {
                    dokumentasi4Ref.downloadUrl.addOnSuccessListener { uri2 ->
                        val dokumentasi4 = uri2.toString()

                        dokumentasi5Ref.putFile(Uri.fromFile(thirdImageFile)).addOnSuccessListener {
                            dokumentasi5Ref.downloadUrl.addOnSuccessListener { uri3 ->
                                val dokumentasi5 = uri3.toString()

                                saveCustomerData(petugas, currentDate, koorX, koorY, koorZ, dokumentasi3, dokumentasi4, dokumentasi5)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveCustomerData(petugas: String, currentDate: Long, koorX: String, koorY: String, koorZ: String, dokumentasi3: String, dokumentasi4: String, dokumentasi5: String) {
        val customerRef = databaseReference.child("listCustomer").child(firebaseKey.toString())

        val data = mapOf(
            "petugas" to petugas,
            "updateInstallDate" to currentDate,
            "xkoordinat" to koorX,
            "ykoordinat" to koorY,
            "zkoordinat" to koorZ,
            "dokumentasi3" to dokumentasi3,
            "dokumentasi4" to dokumentasi4,
            "dokumentasi5" to dokumentasi5,
            "data" to 3
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
                        if (customerData != 2) {
                            displayAnotherData(dataCustomer)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan kesalahan jika mengakses data gagal
                showToast(this@PemasanganGPSActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@PemasanganGPSActivity,
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
                    binding.itemImage1.text =
                        System.currentTimeMillis().toString() + "_konstruksi.jpg"
                } else if (imageNumber == 2) {
                    secondImageFile = file
                    binding.itemImage2.text =
                        System.currentTimeMillis().toString() + "_meter.jpg"
                } else if (imageNumber == 3) {
                    thirdImageFile = file
                    binding.itemImage2.text =
                        System.currentTimeMillis().toString() + "_perspektif.jpg"
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayCustomerData(dataCustomer: CustomerData) {
        // Mengisi tampilan dengan data pelanggan yang ditemukan dari Firebase
        binding.edtVerifikasiPemasangan.apply {
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

        binding.edtRt.apply {
            setText(dataCustomer.rt)
            isEnabled = false
            isFocusable = false
        }

        binding.edtRw.apply {
            setText(dataCustomer.rw)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKelurahan.apply {
            setText(dataCustomer.kelurahan)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKecamatan.apply {
            setText(dataCustomer.kecamatan)
            isEnabled = false
            isFocusable = false
        }

        binding.edtRt.apply {
            setText(dataCustomer.rt)
            isEnabled = false
            isFocusable = false
        }

        binding.edtRw.apply {
            setText(dataCustomer.rw)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKelurahan.apply {
            setText(dataCustomer.kelurahan)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKecamatan.apply {
            setText(dataCustomer.kecamatan)
            isEnabled = false
            isFocusable = false
        }

        binding.edtNomorKl.apply {
            setText(dataCustomer.nomorKL)
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
    }

    @SuppressLint("SetTextI18n")
    private fun displayAnotherData(dataCustomer: CustomerData) {
        binding.edtNomorKl.apply {
            setText(dataCustomer.nomorKL)
            isEnabled = false
            isFocusable = false
        }

        binding.updatedby.apply {
            text = "Update by " + dataCustomer.petugas + " at " + dataCustomer.updateVerifDate.toString()
            isEnabled = false
            isFocusable = false
            visibility = android.view.View.VISIBLE
        }

        binding.edtX.apply {
            setText(dataCustomer.xkoordinat)
            isEnabled = false
            isFocusable = false
        }

        binding.edtY.apply {
            setText(dataCustomer.ykoordinat)
            isEnabled = false
            isFocusable = false
        }

        binding.edtZ.apply {
            setText(dataCustomer.zkoordinat)
            isEnabled = false
            isFocusable = false
        }

        binding.itemImage1.apply {
            text = parsingNameImage(dataCustomer.dokumentasi3)
            setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi1), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.itemImage2.apply {
            text = parsingNameImage(dataCustomer.dokumentasi4)
            setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi2), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.itemImage3.apply {
            text = parsingNameImage(dataCustomer.dokumentasi5)
            setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi3), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            binding.btnSimpan.text = getString(R.string.finish)
            binding.btnSimpan.setOnClickListener {
                navigatePage(this@PemasanganGPSActivity, MainActivity::class.java, true)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}