package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
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
import com.pdam.report.data.SambunganData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemasanganGpsBinding
import com.pdam.report.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class PemasanganGPSActivity : AppCompatActivity() {

    // View Binding
    private val binding by lazy { ActivityPemasanganGpsBinding.inflate(layoutInflater) }

    // Firebase Database
    private val databaseReference = FirebaseDatabase.getInstance().reference

    // Waktu saat ini didapat dari server
    private var currentTime by Delegates.notNull<Long>()

    // Intent-related: Data customer
    private val dataCustomer by lazy {
        intent.getParcelableExtra<SambunganData>(PemasanganSambunganActivity.EXTRA_CUSTOMER_DATA)
    }

    // Intent-related: User (petugas dan/atau admin)
    private val user by lazy { intent.getParcelableExtra<UserData>(PemasanganSambunganActivity.EXTRA_USER_DATA) }

    // Image Handling
    private var imageNumber: Int = 0
    private var firstImageFile: File? = null
    private var secondImageFile: File? = null
    private var thirdImageFile: File? = null

    // Memantau perubahan di semua field yang relevan
    private val isDataChanged = MutableLiveData<Boolean>()

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur tampilan dan tombol back
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this@PemasanganGPSActivity, onBackPressedCallback)

        // Mengatur style action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))
        }

        // Melakukan operasi asynchronous untuk mendapatkan waktu saat ini dari server
        lifecycleScope.launch {
            currentTime = getNetworkTime()

            // Persiapan tombol dan data pengguna
            monitorDataChanges()
            setupButtons()

            // Menampilkan data pelanggan
            displayCustomerData()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            // Menangani tombol back: Navigasi ke PemasanganSambungan dan menambahkan firebaseKey sebagai ekstra dalam Intent
            val intent = Intent(this@PemasanganGPSActivity, PemasanganSambunganActivity::class.java)
            intent.putExtra(PemasanganSambunganActivity.EXTRA_CUSTOMER_DATA, dataCustomer)
            intent.putExtra(PemasanganSambunganActivity.EXTRA_USER_DATA, user)
            startActivity(intent)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Menangani tindakan saat item di ActionBar diklik (tombol back di ActionBar)
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun displayCustomerData() {
        if (dataCustomer != null) {
            val setRole = user?.team == 0
            setCustomerData(dataCustomer!!, setRole)

            if (dataCustomer?.data != 2) {
                displayData(dataCustomer!!, setRole)
            }
        }
    }

    private fun setupButtons() {

        // Menetapkan tindakan yang diambil saat item gambar diklik
        binding.itemImage1.setOnClickListener { imageNumber = 1; startTakePhoto() }
        binding.itemImage2.setOnClickListener { imageNumber = 2; startTakePhoto() }
        binding.itemImage3.setOnClickListener {
            imageNumber = 3;

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
            if (dataCustomer?.data == 2) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun clearData() {

        // Membersihkan semua isian pada field input
        binding.apply {
            edtX.text.clear()
            edtY.text.clear()
            edtZ.text.clear()

            // Mereset teks pada itemImage1 dan itemImage2 menjadi default
            itemImage1.text = getString(R.string.take_photo)
            firstImageFile = null
            itemImage2.text = getString(R.string.take_photo)
            secondImageFile = null
            itemImage3.text = getString(R.string.take_photo)
            thirdImageFile = null
        }
    }

    private fun deleteData() {

        // Mendapatkan referensi ke lokasi data yang akan dihapus
        val listCustomerRef = databaseReference.child("listPemasangan")
        val customerRef = dataCustomer?.firebaseKey?.let { listCustomerRef.child(it) }

        // Mendengarkan perubahan data pada lokasi yang akan dihapus
        customerRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    // Jika data ditemukan, tampilkan dialog konfirmasi untuk menghapus
                    showDeleteConfirmationDialog(customerRef, this@PemasanganGPSActivity)
                } else {
                    showToast(this@PemasanganGPSActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {

                // Jika data tidak ditemukan, tampilkan pesan bahwa data tidak ditemukan
                showToast(
                    this@PemasanganGPSActivity,
                    "${R.string.failed_access_data}: ${error.message}".toInt()
                )
            }
        })
    }


    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not used
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            isDataChanged.value = isDataChanged()
            updateButtonText()
        }

        override fun afterTextChanged(s: Editable?) {
            // Not used
        }
    }

    private fun monitorDataChanges() {
        binding.apply {
            edtVerifikasiPemasangan.addTextChangedListener(textWatcher)
            edtPw.addTextChangedListener(textWatcher)
            edtNomorKl.addTextChangedListener(textWatcher)
            edtNamaPelanggan.addTextChangedListener(textWatcher)
            edtAlamatPelanggan.addTextChangedListener(textWatcher)
            edtRt.addTextChangedListener(textWatcher)
            edtRw.addTextChangedListener(textWatcher)
            edtKelurahan.addTextChangedListener(textWatcher)
            edtKecamatan.addTextChangedListener(textWatcher)
            edtX.addTextChangedListener(textWatcher)
            edtY.addTextChangedListener(textWatcher)
            edtZ.addTextChangedListener(textWatcher)
            edtNomorMeter.addTextChangedListener(textWatcher)
            edtNomorSegel.addTextChangedListener(textWatcher)
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
        x: String,
        y: String,
        z: String,
        nomorMeter: String,
        nomorSegel: String,
    ): Boolean {

        // Memeriksa apakah semua input yang diperlukan tidak kosong
        val isRequiredValid =
            jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorKL.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && x.isNotEmpty() && y.isNotEmpty() && z.isNotEmpty() && nomorMeter.isNotEmpty() && nomorSegel.isNotEmpty()

        // Memeriksa validitas file gambar jika pengguna adalah petugas lapangan
        val isImageFilesValid =
            user?.team == 0 || (firstImageFile != null && secondImageFile != null && thirdImageFile != null)

        // Return true jika semua validasi terpenuhi
        return isRequiredValid && isImageFilesValid
    }

    private fun saveCustomerData() {

        // Mendapatkan data dari bidang input
        val jenisPekerjaan = binding.edtVerifikasiPemasangan.text.toString()
        val pw = binding.edtPw.text.toString()
        val nomorKL = binding.edtNomorKl.text.toString()
        val name = binding.edtNamaPelanggan.text.toString()
        val address = binding.edtAlamatPelanggan.text.toString()
        val rt = binding.edtRt.text.toString()
        val rw = binding.edtRw.text.toString()
        val kelurahan = binding.edtKelurahan.text.toString()
        val kecamatan = binding.edtKecamatan.text.toString()
        val koorX = binding.edtX.text.toString()
        val koorY = binding.edtY.text.toString()
        val koorZ = binding.edtZ.text.toString()
        val nomorMeter = binding.edtNomorMeter.text.toString()
        val nomorSegel = binding.edtNomorSegel.text.toString()

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
                koorX,
                koorY,
                koorZ,
                nomorMeter,
                nomorSegel
            )
        ) {

            // Menampilkan loading indicator dan memblokir layar agar tidak dapat diklik
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showBlockingLayer(window, true)

            // Menyimpan data pelanggan
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
                koorX,
                koorY,
                koorZ,
                nomorMeter,
                nomorSegel,
            )
        } else {

            // Menampilkan pesan jika ada data yang belum diisi
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_dataImage)
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
        koorX: String,
        koorY: String,
        koorZ: String,
        nomorMeter: String,
        nomorSegel: String,
    ) {
        val customerRef = dataCustomer?.firebaseKey?.let {
            databaseReference.child("listPemasangan")
                .child(it)
        }

        if (user?.team != 0) {

            // Bagian untuk tim petugas lapangan
            val storageReference = FirebaseStorage.getInstance().reference

            val dokumentasi3Ref =
                storageReference.child("dokumentasi/${currentTime}_konstruksi.jpg")
            val dokumentasi4Ref =
                storageReference.child("dokumentasi/${currentTime}_meter.jpg")
            val dokumentasi5Ref =
                storageReference.child("dokumentasi/${currentTime}_perspektif.jpg")

            showToast(this@PemasanganGPSActivity, R.string.compressing_image)

            CoroutineScope(Dispatchers.IO).launch {
                val firstImageDeferred = async { firstImageFile?.reduceFileImageInBackground() }
                val secondImageDeferred = async { secondImageFile?.reduceFileImageInBackground() }
                val thirdImageDeferred = async { thirdImageFile?.reduceFileImageInBackground() }

                val dokumentasi3 = uploadImageAndGetUrl(dokumentasi3Ref, firstImageDeferred.await())
                val dokumentasi4 =
                    uploadImageAndGetUrl(dokumentasi4Ref, secondImageDeferred.await())
                val dokumentasi5 = uploadImageAndGetUrl(dokumentasi5Ref, thirdImageDeferred.await())


                if (customerRef != null) {
                    val data = mapOf(
                        "updateInstallDate" to currentTime,
                        "petugas" to user?.username,
                        "jenisPekerjaan" to jenisPekerjaan,
                        "pw" to pw.toInt(),
                        "nomorKL" to nomorKL,
                        "name" to name,
                        "address" to address,
                        "rt" to rt,
                        "rw" to rw,
                        "kelurahan" to kelurahan,
                        "kecamatan" to kecamatan,
                        "xkoordinat" to koorX,
                        "ykoordinat" to koorY,
                        "zkoordinat" to koorZ,
                        "nomorMeter" to nomorMeter,
                        "nomorSegel" to nomorSegel,
                        "dokumentasi3" to dokumentasi3,
                        "dokumentasi4" to dokumentasi4,
                        "dokumentasi5" to dokumentasi5,
                        "data" to 3
                    )

                    customerRef.updateChildren(data).addOnCompleteListener { task ->
                        handleSaveCompletionOrFailure(task)
                    }
                }

            }

        } else {
            //Bagian untuk admin

            // Update data pelanggan yang sudah ada
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
                "xkoordinat" to koorX,
                "ykoordinat" to koorY,
                "zkoordinat" to koorZ,
                "nomorMeter" to nomorMeter,
                "nomorSegel" to nomorSegel
            )

            // Memperbarui data pelanggan yang sudah ada di database
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

    private fun isDataChanged(): Boolean {

        // Bandingkan data yang ada dengan data yang sebelumnya.
        // Jika ada perubahan, kembalikan true, jika tidak, kembalikan false.

        val newData = listOf(
            binding.edtVerifikasiPemasangan.text.toString(),
            binding.edtPw.text.toString(),
            binding.edtNomorKl.text.toString(),
            binding.edtNamaPelanggan.text.toString(),
            binding.edtAlamatPelanggan.text.toString(),
            binding.edtRt.text.toString(),
            binding.edtRw.text.toString(),
            binding.edtKelurahan.text.toString(),
            binding.edtKecamatan.text.toString(),
            binding.edtX.text.toString(),
            binding.edtY.text.toString(),
            binding.edtZ.text.toString(),
            binding.edtNomorMeter.text.toString(),
            binding.edtNomorSegel.text.toString()
        )

        val oldData = listOf(
            dataCustomer?.jenisPekerjaan.toString(),
            dataCustomer?.pw.toString(),
            dataCustomer?.nomorKL.toString(),
            dataCustomer?.name.toString(),
            dataCustomer?.address.toString(),
            dataCustomer?.rt.toString(),
            dataCustomer?.rw.toString(),
            dataCustomer?.kelurahan.toString(),
            dataCustomer?.kecamatan.toString(),
            dataCustomer?.xkoordinat.toString(),
            dataCustomer?.ykoordinat.toString(),
            dataCustomer?.zkoordinat.toString(),
            dataCustomer?.nomorMeter.toString(),
            dataCustomer?.nomorSegel.toString()
        )

        return newData.zip(oldData).any { (new, old) -> isDifferent(new, old) }
    }

    private fun isDifferent(newData: String, oldData: String): Boolean {

        // Fungsi ini membandingkan dua string dan mengembalikan true jika berbeda, false jika sama.
        return newData != oldData
    }

    private fun setCustomerData(dataCustomer: SambunganData, status: Boolean) {

        // Mengisi tampilan dengan data pelanggan yang ditemukan dari Firebase
        binding.apply {
            edtVerifikasiPemasangan.setText(dataCustomer.jenisPekerjaan).apply {
                edVerifikasiPemasangan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtPw.setText(dataCustomer.pw.toString()).apply {
                edPw.apply {
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

            edtNomorKl.setText(dataCustomer.nomorKL).apply {
                edNomorKl.apply {
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

            edtNomorSegel.setText(dataCustomer.nomorSegel).apply {
                edNomorSegel.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setAnotherCustomerData(dataCustomer: SambunganData, status: Boolean) {
        binding.apply {
            edtNomorKl.setText(dataCustomer.nomorKL).apply {
                edNomorKl.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            updatedby.apply {
                text =
                    "Update by " + dataCustomer.petugas + " at " + milisToDateTime(dataCustomer.updateInstallDate)
                isEnabled = status
                isFocusable = status
                visibility = android.view.View.VISIBLE
            }

            edtX.setText(dataCustomer.xkoordinat).apply {
                edX.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtY.setText(dataCustomer.ykoordinat).apply {
                edY.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtZ.setText(dataCustomer.zkoordinat).apply {
                edZ.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            itemImage1.apply {
                text = parsingNameImage(dataCustomer.dokumentasi3)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi3),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            imageView1.apply {
                Glide.with(this@PemasanganGPSActivity)
                    .load(dataCustomer.dokumentasi3)
                    .placeholder(R.drawable.preview_upload_photo)
                    .sizeMultiplier(0.3f)
                    .into(this)
            }

            itemImage2.apply {
                text = parsingNameImage(dataCustomer.dokumentasi4)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi4),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            imageView2.apply {
                Glide.with(this@PemasanganGPSActivity)
                    .load(dataCustomer.dokumentasi4)
                    .placeholder(R.drawable.preview_upload_photo)
                    .sizeMultiplier(0.3f)
                    .into(this)
            }

            itemImage3.apply {
                text = parsingNameImage(dataCustomer.dokumentasi5)
                background = resources.getDrawable(R.drawable.field_disable)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(
                            FullScreenImageDialogFragment(dataCustomer.dokumentasi5),
                            "FullScreenImageDialogFragment"
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }

            imageView3.apply {
                Glide.with(this@PemasanganGPSActivity)
                    .load(dataCustomer.dokumentasi5)
                    .placeholder(R.drawable.preview_upload_photo)
                    .sizeMultiplier(0.3f)
                    .into(this)
            }
        }
    }

    private fun updateButtonText() {
        binding.btnSimpan.apply {
            text = if (isDataChanged.value == true) {
                getString(R.string.simpan)
            } else {
                getString(R.string.finish)
            }
        }
    }

    private fun displayData(dataCustomer: SambunganData, status: Boolean) {
        setAnotherCustomerData(dataCustomer, status)

        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            isDataChanged.value = false
            updateButtonText()

            setOnClickListener {
                if (isDataChanged.value == true) {
                    showDataChangeDialog(
                        this@PemasanganGPSActivity,
                        this@PemasanganGPSActivity::saveCustomerData
                    )
                    return@setOnClickListener
                }
                navigatePage(this@PemasanganGPSActivity, MainActivity::class.java, true)
                finish()
            }
        }
    }

    private lateinit var currentPhotoPath: String

    // Fungsi untuk memulai kamera dan mengambil foto
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        // Membuat file sementara untuk foto
        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@PemasanganGPSActivity,
                "com.pdam.report",
                file
            )
            currentPhotoPath = file.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            // Memulai intent kamera
            launcherIntentCamera.launch(intent)
        }
    }

    // Menangani hasil dari intent pengambilan foto
    @SuppressLint("SetTextI18n")
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Menyimpan foto yang diambil ke file yang sesuai
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                when (imageNumber) {
                    1 -> {
                        firstImageFile = file
                        binding.itemImage1.text =
                            currentTime.toString() + "_konstruksi.jpg"

                        // Menampilkan foto pertama di ImageView menggunakan Glide
                        Glide.with(this@PemasanganGPSActivity)
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
                    }

                    2 -> {
                        secondImageFile = file
                        binding.itemImage2.text =
                            currentTime.toString() + "_meter.jpg"

                        // Menampilkan foto kedua di ImageView menggunakan Glide
                        Glide.with(this@PemasanganGPSActivity)
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

                    3 -> {
                        thirdImageFile = file
                        binding.itemImage3.text =
                            currentTime.toString() + "_perspektif.jpg"

                        // Menampilkan foto ketiga di ImageView menggunakan Glide
                        Glide.with(this@PemasanganGPSActivity)
                            .load(thirdImageFile)
                            .into(binding.imageView3)

                        // Menambahkan listener untuk melihat foto ketiga dalam tampilan layar penuh
                        binding.imageView3.setOnClickListener {
                            supportFragmentManager.beginTransaction()
                                .add(
                                    FullScreenImageDialogFragment(thirdImageFile.toString()),
                                    "FullScreenImageDialogFragment"
                                )
                                .addToBackStack(null)
                                .commit()
                        }
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
                val myFile = uriToFile(uri, this@PemasanganGPSActivity)
                thirdImageFile = myFile

                // Menetapkan teks pada itemImage3 yang menunjukkan gambar yang dipilih
                binding.itemImage3.text = currentTime.toString() + "_perspektif.jpg"
                Glide.with(this@PemasanganGPSActivity)
                    .load(thirdImageFile)
                    .into(binding.imageView3)
            }
        }
    }

    companion object {
        const val EXTRA_USER_DATA = "user_data"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}