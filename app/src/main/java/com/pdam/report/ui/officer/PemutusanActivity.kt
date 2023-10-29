package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.PemutusanData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemutusanBinding
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

class PemutusanActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPemutusanBinding.inflate(layoutInflater) }

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val firebaseKey by lazy { intent.getStringExtra(PemutusanActivity.EXTRA_FIREBASE_KEY) }
    private val customerData by lazy {
        intent.getIntExtra(
            PemutusanActivity.EXTRA_CUSTOMER_DATA,
            0
        )
    }

    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))

        setupDropdownField()
        setupButtons()
        setUser()
        firebaseKey?.let { loadDataFromFirebase(it) }
        Log.d("firebaseKeyOnAddFirst", firebaseKey.toString())
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@PemutusanActivity, MainActivity::class.java)
            finish()
        }
    }

    private fun setUser() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
        }
    }

    private fun setupDropdownField() {
        // Populate a dropdown field with data from resources
        val items1 = resources.getStringArray(R.array.type_of_pw)
        val dropdownField1: AutoCompleteTextView = binding.dropdownPw
        dropdownField1.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items1))
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener { startTakePhoto() }

        binding.btnSimpan.setOnClickListener { saveData() }

        binding.btnHapus.setOnClickListener {
            if (firebaseKey == null) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
        val jenisPekerjaan = binding.edtPemasanganSambungan.text.toString()
        val pw = binding.dropdownPw.text.toString()
        val nomorKL = binding.edtNomorKl.text.toString()
        val name = binding.edtNamaPelanggan.text.toString()
        val address = binding.edtAlamatPelanggan.text.toString()
        val rt = binding.edtRt.text.toString()
        val rw = binding.edtRw.text.toString()
        val kelurahan = binding.edtKelurahan.text.toString()
        val kecamatan = binding.edtKecamatan.text.toString()
        val nomorMeter = binding.edtNomorMeter.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        // Validate input
        if (isInputValid(
                jenisPekerjaan,
                pw,
                nomorKL,
                name,
                address,
                rt,
                rw,
                kelurahan,
                kecamatan,
                nomorMeter,
                keterangan
            )
        ) {
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            uploadImagesAndSaveData(
                currentDate,
                jenisPekerjaan,
                pw,
                nomorKL,
                name,
                address,
                rt,
                rw,
                kelurahan,
                kecamatan,
                nomorMeter,
                keterangan
            )
        } else {
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_dataImage)
        }
    }

    private fun isInputValid(
        jenisPekerjaan: String,
        pw: String,
        nomorKL: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        nomorMeter: String,
        keterangan: String,
    ): Boolean {
        // Check if all required input is valid
        return jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorKL.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && nomorMeter.isNotEmpty() && keterangan.isNotEmpty() && (imageFile != null)
    }

    private fun uploadImagesAndSaveData(
        currentDate: Long,
        jenisPekerjaan: String,
        pw: String,
        nomorKL: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        nomorMeter: String,
        keterangan: String,
    ) {
        val storageReference = FirebaseStorage.getInstance().reference
        val dokumentasi1Ref =
            storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi1_dokumen.jpg")

        lifecycleScope.launch {
            showToast(this@PemutusanActivity, R.string.compressing_image)
            val imageFile = imageFile?.reduceFileImageInBackground()

            // Upload image 1
            dokumentasi1Ref.putFile(Uri.fromFile(imageFile)).addOnSuccessListener {
                dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                    val dokumentasi1 = uri1.toString()
                    saveCustomerData(
                        currentDate,
                        jenisPekerjaan,
                        pw,
                        nomorKL,
                        name,
                        address,
                        rt,
                        rw,
                        kelurahan,
                        kecamatan,
                        nomorMeter,
                        keterangan,
                        dokumentasi1
                    )
                }
            }
        }
    }

    private fun saveCustomerData(
        currentDate: Long,
        jenisPekerjaan: String,
        pw: String,
        nomorKL: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        nomorMeter: String,
        keterangan: String,
        dokumentasi: String,
    ) {
        val newCustomerRef = databaseReference.child("listPemutusan").push()
        val newCustomerId = newCustomerRef.key


        if (newCustomerId != null) {
            val data = PemutusanData(
                firebaseKey = newCustomerId,
                currentDate = currentDate,
                petugas = user.username,
                jenisPekerjaan = jenisPekerjaan,
                pw = pw.toInt(),
                nomorKL = nomorKL,
                name = name,
                address = address,
                rt = rt,
                rw = rw,
                kelurahan = kelurahan,
                kecamatan = kecamatan,
                nomorMeter = nomorMeter,
                keterangan = keterangan,
                dokumentasi = dokumentasi,
                dailyTeam = user.dailyTeam
            )

            newCustomerRef.setValue(data).addOnCompleteListener { task ->
                showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                if (task.isSuccessful) {
                    showToast(this, R.string.save_success)
                } else {
                    showToast(this, R.string.save_failed)
                }
                showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                finish()
            }
        } else {
            // Handle if images are not taken or data is incomplete
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_dataImage)
        }
    }

    private fun loadDataFromFirebase(firebaseKey: String) {
        val customerRef = databaseReference.child("listPemutusan").child(firebaseKey)

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(PemutusanData::class.java)
                    if (dataCustomer != null) {
                        // Jika data pelanggan ditemukan, tampilkan datanya
                        displayCustomerData(dataCustomer)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan kesalahan jika mengakses data gagal
                showToast(
                    this@PemutusanActivity,
                    "${R.string.failed_access_data}: ${error.message}".toInt()
                )
            }
        })
    }

    private fun deleteData() {
        val listCustomerRef = databaseReference.child("listPemutusan")
        val customerRef = firebaseKey?.let { listCustomerRef.child(it) }

        customerRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Show a confirmation dialog for delete
                    showDeleteConfirmationDialog(customerRef, this@PemutusanActivity)
                } else {
                    showToast(this@PemutusanActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(
                    this@PemutusanActivity,
                    "${R.string.failed_access_data}: ${error.message}".toInt()
                )
            }
        })
    }

    private fun clearData() {
        // Clear all input fields
        binding.apply {
            edtPemasanganSambungan.text.clear()
            dropdownPw.text.clear()
            edtNomorKl.text.clear()
            edtNamaPelanggan.text.clear()
            edtAlamatPelanggan.text.clear()
            edtRt.text.clear()
            edtRw.text.clear()
            edtKelurahan.text.clear()
            edtKecamatan.text.clear()
            edtNomorMeter.text.clear()
            edtKeterangan.text.clear()
            itemImage1.text = getString(R.string.take_photo)
            imageFile = null
        }
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@PemutusanActivity,
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
                imageFile = file
                binding.itemImage1.text = System.currentTimeMillis().toString() + "_dokumentasi.jpg"

                Glide.with(this@PemutusanActivity)
                    .load(imageFile)
                    .into(binding.imageView1)

                binding.imageView1.setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(imageFile.toString()),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    private fun displayCustomerData(dataCustomer: PemutusanData) {
        binding.apply {
            edtPemasanganSambungan.setText(dataCustomer.jenisPekerjaan).apply {
                edPemasanganSambungan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            addedby.apply {
                text =
                    "Added by " + dataCustomer.petugas + " at " + dataCustomer.currentDate.toString()
                isEnabled = false
                isFocusable = false
                visibility = android.view.View.VISIBLE
            }

            dropdownPw.setText(dataCustomer.pw.toString()).apply {
                edPw.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtNomorKl.setText(dataCustomer.nomorKL).apply {
                edNomorKl.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtNamaPelanggan.setText(dataCustomer.name).apply {
                edNamaPelanggan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtAlamatPelanggan.setText(dataCustomer.address).apply {
                edAlamatPelanggan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtRt.setText(dataCustomer.rt).apply {
                edRt.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtRw.setText(dataCustomer.rw).apply {
                edRw.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtKelurahan.setText(dataCustomer.kelurahan).apply {
                edKelurahan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtKecamatan.setText(dataCustomer.kecamatan).apply {
                edKecamatan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtNomorMeter.setText(dataCustomer.nomorMeter).apply {
                edNomorMeter.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            edtKeterangan.setText(dataCustomer.keterangan).apply {
                edKeterangan.apply {
                    isEnabled = false
                    isFocusable = false
                }
            }

            binding.btnSimpan.text = getString(R.string.finish)
            binding.btnSimpan.setOnClickListener {
                navigatePage(this@PemutusanActivity, MainActivity::class.java)
                finish()
            }

            itemImage1.apply {
                text = parsingNameImage(dataCustomer.dokumentasi)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            binding.imageView1.apply {
                Glide.with(this@PemutusanActivity)
                    .load(dataCustomer.dokumentasi)
                    .placeholder(R.drawable.preview_upload_photo)
                    .sizeMultiplier(0.3f)
                    .into(this)
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

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}