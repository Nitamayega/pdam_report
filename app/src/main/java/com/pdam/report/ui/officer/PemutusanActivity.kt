package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.PemutusanData
import com.pdam.report.data.SambunganData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemutusanBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.pdam.report.utils.*
import kotlin.properties.Delegates

class PemutusanActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPemutusanBinding.inflate(layoutInflater) }

    private val databaseReference = FirebaseDatabase.getInstance().reference

    // Intent-related
    private val firebaseKey by lazy {
        intent.getStringExtra("firebase_key")
    }

    private var currentTime by Delegates.notNull<Long>()

    private val userManager by lazy { UserManager() }
    private var user: UserData? = null

    private var imageFile: File? = null

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            currentTime = getNetworkTime()
            setContentView(binding.root)
            onBackPressedDispatcher.addCallback(this@PemutusanActivity, onBackPressedCallback)

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))

            setupDropdownField()
            setupButtons()
            setUser()
            firebaseKey?.let { loadDataFromFirebase(it) }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@PemutusanActivity, MainActivity::class.java, true)
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
        try {
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
                saveCustomerData(
                    currentTime,
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
                showToast(this@PemutusanActivity, R.string.fill_all_data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

        // Memeriksa apakah semua input yang diperlukan tidak kosong
        val isRequiredValid =
            jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorKL.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && nomorMeter.isNotEmpty() && keterangan.isNotEmpty()

        // Memeriksa validitas file gambar jika pengguna adalah petugas lapangan
        val isImageFilesValid = user?.team == 0 || imageFile != null

        // Return true jika semua validasi terpenuhi
        return isRequiredValid && isImageFilesValid
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
    ) {
        if (user?.team != 0) {
            val storageReference = FirebaseStorage.getInstance().reference
            val dokumentasi1Ref =
                storageReference.child("dokumentasi/${currentDate}_dokumen.jpg")

            showToast(this@PemutusanActivity, R.string.compressing_image)
            CoroutineScope(Dispatchers.IO).launch {
                val imageFile = imageFile?.reduceFileImageInBackground()

                // Upload image 1
                dokumentasi1Ref.putFile(Uri.fromFile(imageFile)).addOnSuccessListener {
                    dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                        val dokumentasi1 = uri1.toString()
                        val newCustomerRef = databaseReference.child("listPemutusan").push()
                        val newCustomerId = newCustomerRef.key

                        if (newCustomerId != null) {
                            val data = mapOf(
                                "firebaseKey" to newCustomerId,
                                "currentDate" to currentDate,
                                "petugas" to user?.username,
                                "dailyTeam" to user?.dailyTeam,
                                "jenisPekerjaan" to jenisPekerjaan,
                                "pw" to pw.toInt(),
                                "nomorKL" to nomorKL,
                                "name" to name,
                                "address" to address,
                                "rt" to rt,
                                "rw" to rw,
                                "kelurahan" to kelurahan,
                                "kecamatan" to kecamatan,
                                "nomorMeter" to nomorMeter,
                                "keterangan" to keterangan,
                                "dokumentasi" to dokumentasi1
                            )

                            newCustomerRef.setValue(data).addOnCompleteListener { task ->
                                handleSaveCompletionOrFailure(task)
                            }
                        }
                    }
                }
            }
        } else {
            //Bagian admin
            val customerRef =
                firebaseKey?.let { databaseReference.child("listPemutusan").child(it) }

            val updatedValues = mapOf(
                "jenisPekerjaan" to jenisPekerjaan,
                "pw" to pw.toInt(),
                "nomorKL" to nomorKL,
                "name" to name,
                "address" to address,
                "rt" to rt,
                "rw" to rw,
                "kelurahan" to kelurahan,
                "kecamatan" to kecamatan,
                "nomorMeter" to nomorMeter,
                "keterangan" to keterangan
            )

            customerRef?.updateChildren(updatedValues)?.addOnCompleteListener { task ->
                handleSaveCompletionOrFailure(task)
            }
        }
    }

    private fun handleSaveCompletionOrFailure(task: Task<Void>) {
        // Menampilkan atau menyembunyikan loading, menampilkan pesan sukses atau gagal, dan menyelesaikan aktivitas
        showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
        if (task.isSuccessful) {
            showToast(this, R.string.save_success)
        } else {
            showToast(this, R.string.save_failed)
        }
        showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
        finish()
    }

    private fun loadDataFromFirebase(firebaseKey: String) {
        val customerRef = databaseReference.child("listPemutusan").child(firebaseKey)

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(PemutusanData::class.java)
                    if (dataCustomer != null) {
                        if (user?.team == 0) displayData(dataCustomer, true) else displayData(
                            dataCustomer,
                            false
                        )
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
                binding.itemImage1.text = currentTime.toString() + "_dokumentasi.jpg"

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

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setCustomerData(dataCustomer: PemutusanData, status: Boolean) {
        binding.apply {
            edtPemasanganSambungan.setText(dataCustomer.jenisPekerjaan).apply {
                edPemasanganSambungan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            updatedby.apply {
                text =
                    "Added by " + dataCustomer.petugas + " at " + milisToDateTime(dataCustomer.currentDate)
                isEnabled = status
                isFocusable = status
                visibility = VISIBLE
            }

            dropdownPw.apply {
                setText(dataCustomer.pw.toString())
                if (!status) setAdapter(null)
            }.apply {
                edPw.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtNomorKl.setText(dataCustomer.nomorKL).apply {
                edNomorKl.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtNamaPelanggan.setText(dataCustomer.name).apply {
                edNamaPelanggan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtAlamatPelanggan.setText(dataCustomer.address).apply {
                edAlamatPelanggan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtRt.setText(dataCustomer.rt).apply {
                edRt.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtRw.setText(dataCustomer.rw).apply {
                edRw.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtKelurahan.setText(dataCustomer.kelurahan).apply {
                edKelurahan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtKecamatan.setText(dataCustomer.kecamatan).apply {
                edKecamatan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtNomorMeter.setText(dataCustomer.nomorMeter).apply {
                edNomorMeter.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtKeterangan.setText(dataCustomer.keterangan).apply {
                edKeterangan.apply {
                    isEnabled = status
                    isFocusable = status
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

    private fun displayData(dataCustomer: PemutusanData, status: Boolean) {
        setCustomerData(dataCustomer, status)
        binding.btnSimpan.apply {
            setOnClickListener {
                if (status) {
                    setupDropdownField()
                }
                if (isDataChange(
                        dataCustomer,
                        binding.edtPemasanganSambungan.text.toString(),
                        binding.dropdownPw.text.toString(),
                        binding.edtNomorKl.text.toString(),
                        binding.edtNamaPelanggan.text.toString(),
                        binding.edtAlamatPelanggan.text.toString(),
                        binding.edtRt.text.toString(),
                        binding.edtRw.text.toString(),
                        binding.edtKelurahan.text.toString(),
                        binding.edtKecamatan.text.toString(),
                        binding.edtKeterangan.text.toString()
                    )
                ) {
                    text = getString(R.string.simpan)
                    showDataChangeDialog(this@PemutusanActivity, ::saveData)
                    return@setOnClickListener
                }
                text = getString(R.string.finish)
                navigatePage(this@PemutusanActivity, MainActivity::class.java, true)
                finish()
            }
        }
    }

    private fun isDataChange(
        data: PemutusanData,
        jenisPekerjaan: String,
        pw: String,
        nomorKL: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        keterangan: String
    ): Boolean {
        // Membandingkan setiap data apakah ada perubahan atau tidak
        return jenisPekerjaan != data.jenisPekerjaan ||
                pw != data.pw.toString() ||
                nomorKL != data.nomorKL ||
                name != data.name ||
                address != data.address ||
                rt != data.rt ||
                rw != data.rw ||
                kelurahan != data.kelurahan ||
                kecamatan != data.kecamatan ||
                keterangan != data.keterangan
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigatePage(this, MainActivity::class.java, true)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
    }
}