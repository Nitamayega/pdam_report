package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemutusanBinding
import com.pdam.report.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class PemutusanActivity : AppCompatActivity() {

    // View Binding
    private val binding by lazy { ActivityPemutusanBinding.inflate(layoutInflater) }

    // Firebase Database
    private val databaseReference = FirebaseDatabase.getInstance().reference

    // Waktu saat ini didapat dari server
    private var currentTime by Delegates.notNull<Long>()

    // Intent-related: Firebase key
    private val firebaseKey by lazy {
        intent.getStringExtra("firebase_key")
    }

    // User-related
    private val userManager by lazy { UserManager() }
    private var user: UserData? = null

    // Image Handling
    private var imageNumber: Int = 0
    private var firstImageFile: File? = null
    private var secondImageFile: File? = null

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur tampilan dan tombol back
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this@PemutusanActivity, onBackPressedCallback)

        // Mengatur style action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))
        }

        // Mengambil data user
        setUser()

        lifecycleScope.launch {
            currentTime = getNetworkTime()

            // Mengatur dropdown field dan tombol
            setupDropdownField()
            setupButtons()

            // Mengambil data dari Firebase
            firebaseKey?.let { loadDataFromFirebase(it) }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@PemutusanActivity, MainActivity::class.java, true)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Menangani tindakan saat item di ActionBar diklik (tombol back di ActionBar)
        if (item.itemId == android.R.id.home) {
            navigatePage(this, MainActivity::class.java, true)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setUser() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
        }
    }

    private fun setupButtons() {

        // Menetapkan tindakan yang diambil saat item gambar diklik
        binding.itemImage1.setOnClickListener {
            imageNumber = 1

            // Menampilkan dialog untuk memilih sumber gambar
            AlertDialog.Builder(this).apply {
                setTitle("Pilih Sumber Gambar")
                setItems(arrayOf("Kamera", "Galeri")) { dialog, which ->
                    when (which) {
                        0 -> {
                            startTakePhoto()
                        }

                        1 -> {
                            startGallery()
                        }
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.cancel, null)
            }.create().show()
        }

        binding.itemImage2.setOnClickListener {
            imageNumber = 2

            // Menampilkan dialog untuk memilih sumber gambar
            AlertDialog.Builder(this).apply {
                setTitle("Pilih Sumber Gambar")
                setItems(arrayOf("Kamera", "Galeri")) { dialog, which ->
                    when (which) {
                        0 -> {
                            startTakePhoto()
                        }

                        1 -> {
                            startGallery()
                        }
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.cancel, null)
            }.create().show()
        }

        // Menetapkan tindakan yang dilakukan saat tombol "Simpan" diklik
        binding.btnSimpan.setOnClickListener { saveCustomerData() }

        // Menetapkan tindakan yang dilakukan saat tombol "Hapus" diklik
        binding.btnHapus.setOnClickListener {
            if (firebaseKey == null) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun setupDropdownField() {
        // Populate a dropdown field with data from resources
        val items1 = resources.getStringArray(R.array.type_of_pw)
        val dropdownField1: AutoCompleteTextView = binding.dropdownPw
        dropdownField1.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items1))
    }

    private fun clearData() {

        // Membersihkan semua isian pada field input
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

            // Mereset teks pada itemImage1 menjadi default
            itemImage1.text = getString(R.string.take_photo)
            firstImageFile = null
            itemImage2.text = getString(R.string.take_photo)
            secondImageFile = null

        }
    }

    private fun deleteData() {

        // Mendapatkan referensi ke lokasi data yang akan dihapus
        val listCustomerRef = databaseReference.child("listPemutusan")
        val customerRef = firebaseKey?.let { listCustomerRef.child(it) }

        // Mendengarkan perubahan data pada lokasi yang akan dihapus
        customerRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    // Jika data ditemukan, tampilkan dialog konfirmasi untuk menghapus
                    showDeleteConfirmationDialog(customerRef, this@PemutusanActivity)
                } else {
                    showToast(this@PemutusanActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {

                // Jika terjadi kesalahan saat mengakses data, tampilkan pesan kesalahan
                showToast(
                    this@PemutusanActivity,
                    "${R.string.failed_access_data}: ${error.message}".toInt()
                )
            }
        })
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
        val isImageFilesValid = user?.team == 0 || (firstImageFile != null && secondImageFile != null)

        // Return true jika semua validasi terpenuhi
        return isRequiredValid && isImageFilesValid
    }

    private fun saveCustomerData() {
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

                // Menampilkan loading indicator dan memblokir layar agar tidak dapat diklik
                showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                showBlockingLayer(window, true)

                setValueCustomerData(
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

                // Menampilkan pesan jika ada data yang belum diisi
                showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                showToast(this@PemutusanActivity, R.string.fill_all_data)
            }
        } catch (e: Exception) {

            // Menangani pengecualian jika terjadi kesalahan
            e.printStackTrace()
        }

    }

    private fun setValueCustomerData(
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
                storageReference.child("dokumentasi/${currentTime}_sebelumcabut_${user?.username}.jpg")
            val dokumentasi2Ref =
                storageReference.child("dokumentasi/${currentTime}_setelahcabut_${user?.username}.jpg")

            showToast(this@PemutusanActivity, R.string.compressing_image)

            CoroutineScope(Dispatchers.IO).launch {
                val _firstImageFile = async { firstImageFile?.reduceFileImageInBackground() }
                val _secondImageFile = async { secondImageFile?.reduceFileImageInBackground() }

                val firstImageFile = _firstImageFile.await()
                val secondImageFile = _secondImageFile.await()

                // Upload image 1
                dokumentasi1Ref.putFile(Uri.fromFile(firstImageFile)).addOnSuccessListener {
                    dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                        val dokumentasi1 = uri1.toString()

                        // Upload image 2
                        dokumentasi2Ref.putFile(Uri.fromFile(secondImageFile))
                            .addOnSuccessListener {
                                dokumentasi2Ref.downloadUrl.addOnSuccessListener { uri2 ->
                                    val dokumentasi2 = uri2.toString()

                                    // Membuat referensi baru di Firebase Database
                                    val newCustomerRef =
                                        databaseReference.child("listPemutusan").push()
                                    val newCustomerId = newCustomerRef.key

                                    if (newCustomerId != null) {
                                        val data = mapOf(
                                            "firebaseKey" to newCustomerId,
                                            "currentDate" to currentTime,
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
                                            "dokumentasi1" to dokumentasi1,
                                            "dokumentasi2" to dokumentasi2
                                        )

                                        newCustomerRef.setValue(data)
                                            .addOnCompleteListener { task ->
                                                handleSaveCompletionOrFailure(task)
                                            }
                                    }
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
        showBlockingLayer(window, false)
        finish()
    }


    private fun isDataChanged(data: PemutusanData): Boolean {

        // Bandingkan data yang ada dengan data yang sebelumnya.
        // Jika ada perubahan, kembalikan true; jika tidak, kembalikan false.

        val newData = listOf(
            binding.edtPemasanganSambungan.text.toString(),
            binding.dropdownPw.text.toString(),
            binding.edtNomorKl.text.toString(),
            binding.edtNamaPelanggan.text.toString(),
            binding.edtAlamatPelanggan.text.toString(),
            binding.edtRt.text.toString(),
            binding.edtRw.text.toString(),
            binding.edtKelurahan.text.toString(),
            binding.edtKecamatan.text.toString(),
            binding.edtNomorMeter.text.toString(),
            binding.edtKeterangan.text.toString()
        )

        val oldData = listOf(
            data.jenisPekerjaan,
            data.pw.toString(),
            data.nomorKL,
            data.name,
            data.address,
            data.rt,
            data.rw,
            data.kelurahan,
            data.kecamatan,
            data.nomorMeter,
            data.keterangan
        )

        return newData.zip(oldData).any { (new, old) -> isDifferent(new, old) }
    }

    private fun isDifferent(newData: String, oldData: String): Boolean {

        // Fungsi ini membandingkan dua string dan mengembalikan true jika berbeda, false jika sama.
        return newData != oldData
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
                navigatePage(this@PemutusanActivity, MainActivity::class.java, true)
                finish()
            }

            itemImage1.apply {
                text = parsingNameImage(dataCustomer.dokumentasi1)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi1),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            binding.imageView1.apply {
                Glide.with(this@PemutusanActivity)
                    .load(dataCustomer.dokumentasi1)
                    .placeholder(R.drawable.preview_upload_photo)
                    .sizeMultiplier(0.3f)
                    .into(this)
            }

            itemImage2.apply {
                text = parsingNameImage(dataCustomer.dokumentasi2)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi2),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            binding.imageView2.apply {
                Glide.with(this@PemutusanActivity)
                    .load(dataCustomer.dokumentasi2)
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

                if (isDataChanged(dataCustomer)) {
                    text = getString(R.string.simpan)
                    showDataChangeDialog(
                        this@PemutusanActivity,
                        this@PemutusanActivity::saveCustomerData
                    )
                    return@setOnClickListener
                }
                text = getString(R.string.finish)
                navigatePage(this@PemutusanActivity, MainActivity::class.java, true)
                finish()
            }
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
                if (imageNumber == 1) {
                    firstImageFile = file
                    binding.itemImage1.text =
                        currentTime.toString() + "_sebelum.jpg"

                    // Menampilkan foto pertama di ImageView menggunakan Glide
                    Glide.with(this@PemutusanActivity)
                        .load(firstImageFile)
                        .into(binding.imageView1)

                    // Menambahkan listener untuk melihat foto pertama dalam tampilan layar penuh
                    binding.imageView1.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(
                                FullScreenImageDialogFragment(firstImageFile.toString()),
                                "FullScreenImageDialogFragment"
                            )
                            .addToBackStack(null)
                            .commit()
                    }

                } else if (imageNumber == 2) {
                    secondImageFile = file
                    binding.itemImage2.text =
                        currentTime.toString() + "_sesudah.jpg"

                    // Menampilkan foto kedua di ImageView menggunakan Glide
                    Glide.with(this@PemutusanActivity)
                        .load(secondImageFile)
                        .into(binding.imageView2)

                    // Menambahkan listener untuk melihat foto kedua dalam tampilan layar penuh
                    binding.imageView2.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(
                                FullScreenImageDialogFragment(secondImageFile.toString()),
                                "FullScreenImageDialogFragment"
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
    }

    private fun startGallery() {

        // Membuat intent untuk mendapatkan gambar dari galeri
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        // Memulai intent pemilihan galeri
        launcherIntentGallery.launch(intent)
    }

    @SuppressLint("SetTextI18n")
    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {

            // Mendapatkan URI gambar yang dipilih
            val selectedImg = result.data?.data as Uri
            selectedImg.let { uri ->

                // Mengonversi URI ke file dan menetapkannya ke thirdImageFile
                val myFile = uriToFile(uri, this@PemutusanActivity)

                if (imageNumber == 1) {
                    firstImageFile = myFile

                    // Menetapkan teks pada itemImage3 yang menunjukkan gambar yang dipilih
                    binding.itemImage1.text = currentTime.toString() + "_sebelum.jpg"
                    Glide.with(this@PemutusanActivity)
                        .load(firstImageFile)
                        .into(binding.imageView1)

                    // Menambahkan listener untuk melihat foto ketiga dalam tampilan layar penuh
                    binding.imageView1.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(
                                FullScreenImageDialogFragment(firstImageFile.toString()),
                                "FullScreenImageDialogFragment"
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                } else {
                    secondImageFile = myFile

                    // Menetapkan teks pada itemImage3 yang menunjukkan gambar yang dipilih
                    binding.itemImage2.text = currentTime.toString() + "_sesudah.jpg"
                    Glide.with(this@PemutusanActivity)
                        .load(secondImageFile)
                        .into(binding.imageView2)

                    // Menambahkan listener untuk melihat foto ketiga dalam tampilan layar penuh
                    binding.imageView2.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(
                                FullScreenImageDialogFragment(secondImageFile.toString()),
                                "FullScreenImageDialogFragment"
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
    }
}