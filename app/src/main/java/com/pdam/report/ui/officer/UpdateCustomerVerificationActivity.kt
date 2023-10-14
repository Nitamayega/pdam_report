package com.pdam.report.ui.officer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.CustomerData
import com.pdam.report.databinding.ActivityUpdateCustomerVerificationBinding
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import java.io.File

class UpdateCustomerVerificationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUpdateCustomerVerificationBinding.inflate(layoutInflater) }
    private var imageFile: File? = null

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val firebaseKey by lazy { intent.getStringExtra(AddFirstDataActivity.EXTRA_FIREBASE_KEY) }
    private val customerData by lazy { intent.getIntExtra(AddFirstDataActivity.EXTRA_CUSTOMER_DATA, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firebaseKey?.let {
            loadDataFromFirebase(it)
        }
        setupButtons()
    }

    private fun setupButtons() {
        binding.itemImage.setOnClickListener { startTakePhoto() }
        binding.btnSimpan.setOnClickListener { saveData() }
        binding.btnHapus.setOnClickListener {
            if (customerData == 2) {
                binding.btnHapus.setOnClickListener { clearData() }
            } else {
                binding.btnHapus.setOnClickListener { deleteData() }
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
                    showDeleteConfirmationDialog(customerRef)
                } else {
                    showToast(this@UpdateCustomerVerificationActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@UpdateCustomerVerificationActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private fun showDeleteConfirmationDialog(customerRef: DatabaseReference) {
        AlertDialog.Builder(this@UpdateCustomerVerificationActivity).apply {
            setTitle(R.string.delete_data)
            setMessage(R.string.delete_confirmation)
            setPositiveButton(R.string.delete) { _, _ ->
                // Confirm and proceed with deletion
                deleteCustomerData(customerRef)
            }
            setNegativeButton(R.string.cancel, null)
        }.create().show()
    }

    private fun deleteCustomerData(customerRef: DatabaseReference) {
        customerRef.removeValue()
            .addOnSuccessListener {
                showToast(this@UpdateCustomerVerificationActivity, R.string.delete_success)
                finish()
            }
            .addOnFailureListener { error ->
                showToast(this@UpdateCustomerVerificationActivity, "${R.string.delete_failed}: ${error.message}".toInt())
            }
    }

    private fun clearData() {
        // Clear all input fields
        binding.edtKeterangan.text.clear()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, AddFirstDataActivity::class.java)
        startActivity(intent)
        super.onBackPressed()
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
        val koorX = binding.edtX.text.toString()
        val koorY = binding.edtY.text.toString()
        val koorZ = binding.edtZ.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        // Validate input
        if (isInputValid(koorX, koorY, koorZ, keterangan)) {
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            uploadImagesAndSaveData(currentDate, koorX, koorY, koorZ, keterangan)
        } else {
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_data)
        }
    }

    private fun isInputValid(koorX: String, koorY: String, koorZ: String, keterangan: String): Boolean {
        // Check if all required input is valid
        return koorX.isNotEmpty() && koorY.isNotEmpty() && koorZ.isNotEmpty() && keterangan.isNotEmpty() && imageFile != null
    }

    private fun uploadImagesAndSaveData(currentDate: Long, koorX: String, koorY: String, koorZ: String, keterangan: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val dokumentasi4Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi4.jpg")

        // Upload image 4
        dokumentasi4Ref.putFile(Uri.fromFile(imageFile)).addOnSuccessListener {
            dokumentasi4Ref.downloadUrl.addOnSuccessListener { uri1 ->
                val dokumentasi4 = uri1.toString()

                // After successfully obtaining image URLs, save the data to Firebase
                saveCustomerData(currentDate, koorX, koorY, koorZ, keterangan, dokumentasi4)
            }
        }
    }

    private fun saveCustomerData(currentDate: Long, koorX: String, koorY: String, koorZ: String, keterangan: String, dokumentasi4: String) {
        val customerRef = databaseReference.child("listCustomer").child(firebaseKey.toString())

        val data = mapOf(
            "updateInstallDate" to currentDate,
            "xKoordinat" to koorX,
            "yKoordinat" to koorY,
            "zKoordinat" to koorZ,
            "keterangan3" to keterangan,
            "dokumentasi4" to dokumentasi4,
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
                        if (customerData != 1) {
                            displayAnotherData(dataCustomer)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan kesalahan jika mengakses data gagal
                showToast(this@UpdateCustomerVerificationActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@UpdateCustomerVerificationActivity,
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
            // After successfully capturing an image, assign it to the appropriate file
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                imageFile = file
                binding.itemImage.text = System.currentTimeMillis().toString() + "_dokumentasi3.jpg"
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigatePage(this, AddFirstDataActivity::class.java, true)
            finish()
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

        binding.edtKeterangan.apply {
            setText(dataCustomer.keterangan2)
        }
    }

    private fun displayAnotherData(dataCustomer: CustomerData) {
        binding.edtNomorKl.apply {
            setText(dataCustomer.nomorKL)
            isEnabled = false
            isFocusable = false
        }

        binding.edtX.apply {
            setText(dataCustomer.xKoordinat)
            isEnabled = false
            isFocusable = false
        }

        binding.edtY.apply {
            setText(dataCustomer.yKoordinat)
            isEnabled = false
            isFocusable = false
        }

        binding.edtZ.apply {
            setText(dataCustomer.zKooridnat)
            isEnabled = false
            isFocusable = false
        }

        binding.edtKeterangan.apply {
            setText(dataCustomer.keterangan3)
            isEnabled = false
            isFocusable = false
        }

        binding.itemImage.apply {
            text = dataCustomer.dokumentasi4
            isEnabled = false
        }

        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            binding.btnSimpan.text = getString(R.string.finish)
            binding.btnSimpan.setOnClickListener {
                navigatePage(this@UpdateCustomerVerificationActivity, MainActivity::class.java, true)
            }
        }
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}