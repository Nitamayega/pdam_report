package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.pdam.report.databinding.ActivityPemasanganKelayakanBinding
import com.pdam.report.utils.FullScreenImageDialogFragment
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.getNetworkTime
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.parsingNameImage
import com.pdam.report.utils.reduceFileImageInBackground
import com.pdam.report.utils.showBlockingLayer
import com.pdam.report.utils.showDataChangeDialog
import com.pdam.report.utils.showDeleteConfirmationDialog
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class PemasanganKelayakanActivity : AppCompatActivity() {

    // View Binding
    private val binding by lazy { ActivityPemasanganKelayakanBinding.inflate(layoutInflater) }

    // Firebase Database
    private val databaseReference = FirebaseDatabase.getInstance().reference

    // Waktu saat ini didapat dari server
    private var currentTime by Delegates.notNull<Long>()

    // Intent-related: Data customer
    private val dataCustomer by lazy {
        intent.getParcelableExtra("customer_data") as? SambunganData
    }

    // Intent-related: User (petugas dan/atau admin)
    private val user by lazy {
        intent.getParcelableExtra("user_data") as? UserData
    }

    // Image Handling
    private var imageNumber: Int = 0
    private var firstImageFile: File? = null
    private var secondImageFile: File? = null

    // Memantau perubahan di semua field yang relevan
    private val isDataChanged = MutableLiveData<Boolean>()

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur tampilan dan tombol back
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(
            this@PemasanganKelayakanActivity,
            onBackPressedCallback
        )

        // Mengatur style action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))
        }

        // Melakukan operasi asynchronous untuk mendapatkan waktu saat ini dari server
        lifecycleScope.launch {
            currentTime = getNetworkTime()

            // Persiapan dropdown, memantau perubahan data, dan tombol-tombol
            setupDropdownField()
            monitorDataChanges()
            setupButtons()

            // Menampilkan data pelanggan
            displayCustomerData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Menghapus callback yang terkait dengan aksi tombol back
        onBackPressedCallback.remove()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Menangani tombol back: Navigasi ke MainActivity dan menyelesaikan Activity saat ini
            navigatePage(this@PemasanganKelayakanActivity, MainActivity::class.java, true)
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

    private fun displayCustomerData() {
        if (dataCustomer != null) {

            // Memeriksa apakah pengguna adalah petugas dan status data pelanggan "Layak"
            val status = user?.team == 0 && dataCustomer?.keterangan1 == "Layak"

            // Menampilkan data pelanggan berdasarkan status penilaian
            displayData(dataCustomer!!, status)
        }
    }

    private fun setupButtons() {

        // Menetapkan tindakan yang diambil saat item gambar diklik
        binding.itemImage1.setOnClickListener { imageNumber = 1; startTakePhoto() }
        binding.itemImage2.setOnClickListener { imageNumber = 2; startTakePhoto() }

        // Menetapkan tindakan yang dilakukan saat tombol "Simpan" diklik
        binding.btnSimpan.setOnClickListener { saveCustomerData() }

        // Menetapkan tindakan yang dilakukan saat tombol "Hapus" diklik
        binding.btnHapus.setOnClickListener {
            if (dataCustomer?.firebaseKey == null) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun setupDropdownField() {

        // Mengambil array data dari resources untuk setiap dropdown
        val items1 = resources.getStringArray(R.array.type_of_work)
        val items2 = resources.getStringArray(R.array.type_of_pw)
        val items3 = resources.getStringArray(R.array.type_of_ket)

        // Mendefinisikan AutoCompleteTextView untuk setiap dropdown
        val dropdownField1 = binding.dropdownJenisPekerjaan
        val dropdownField2 = binding.dropdownPw
        val dropdownField3 = binding.dropdownKeterangan

        // Membuat adapter ArrayAdapter<String> untuk setiap dropdown dengan menggunakan layout default
        val adapter1 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items1)
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items2)
        val adapter3 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items3)

        // Mengatur adapter untuk setiap dropdown
        dropdownField1.setAdapter(adapter1)
        dropdownField2.setAdapter(adapter2)
        dropdownField3.setAdapter(adapter3)
    }

    private fun clearData() {

        // Membersihkan semua isian pada field input
        binding.apply {
            dropdownJenisPekerjaan.text.clear()
            dropdownPw.text.clear()
            edtNomorRegistrasi.text.clear()
            edtNamaPelanggan.text.clear()
            edtAlamatPelanggan.text.clear()
            edtRt.text.clear()
            edtRw.text.clear()
            edtKelurahan.text.clear()
            edtKecamatan.text.clear()
            dropdownKeterangan.text.clear()

            // Mereset teks pada itemImage1 dan itemImage2 menjadi default
            itemImage1.text = getString(R.string.take_photo)
            firstImageFile = null
            itemImage2.text = getString(R.string.take_photo)
            secondImageFile = null
        }
    }

    private fun deleteData() {

        // Mendapatkan referensi ke lokasi data yang akan dihapus
        val listCustomerRef = databaseReference.child("listPemasangan")
        val customerRef = listCustomerRef.child(dataCustomer!!.firebaseKey)

        // Mendengarkan perubahan data pada lokasi yang akan dihapus
        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    // Jika data ditemukan, tampilkan dialog konfirmasi untuk menghapus
                    showDeleteConfirmationDialog(customerRef, this@PemasanganKelayakanActivity)
                } else {

                    // Jika data tidak ditemukan, tampilkan pesan bahwa data tidak ditemukan
                    showToast(this@PemasanganKelayakanActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {

                // Jika terjadi kesalahan saat mengakses data, tampilkan pesan kesalahan
                showToast(
                    this@PemasanganKelayakanActivity,
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
        binding.dropdownJenisPekerjaan.addTextChangedListener(textWatcher)
        binding.dropdownPw.addTextChangedListener(textWatcher)
        binding.edtNomorRegistrasi.addTextChangedListener(textWatcher)
        binding.edtNamaPelanggan.addTextChangedListener(textWatcher)
        binding.edtAlamatPelanggan.addTextChangedListener(textWatcher)
        binding.edtRt.addTextChangedListener(textWatcher)
        binding.edtRw.addTextChangedListener(textWatcher)
        binding.edtKelurahan.addTextChangedListener(textWatcher)
        binding.edtKecamatan.addTextChangedListener(textWatcher)
        binding.dropdownKeterangan.addTextChangedListener(textWatcher)
    }

    private fun isInputValid(
        jenisPekerjaan: String,
        pw: String,
        nomorRegistrasi: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        keterangan: String,
    ): Boolean {

        // Memeriksa apakah semua input yang diperlukan tidak kosong
        val isRequiredValid =
            jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorRegistrasi.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && keterangan.isNotEmpty()

        // Memeriksa validitas file gambar jika pengguna adalah petugas lapangan
        val isImageFilesValid =
            user?.team == 0 || (firstImageFile != null && secondImageFile != null)

        // Return true jika semua validasi terpenuhi
        return isRequiredValid && isImageFilesValid
    }

    private fun saveCustomerData() {
        try {

            // Mendapatkan nilai dari field input
            val jenisPekerjaan = binding.dropdownJenisPekerjaan.text.toString()
            val pw = binding.dropdownPw.text.toString()
            val nomorRegistrasi = binding.edtNomorRegistrasi.text.toString()
            val name = binding.edtNamaPelanggan.text.toString()
            val address = binding.edtAlamatPelanggan.text.toString()
            val rt = binding.edtRt.text.toString()
            val rw = binding.edtRw.text.toString()
            val kelurahan = binding.edtKelurahan.text.toString()
            val kecamatan = binding.edtKecamatan.text.toString()
            val keterangan = binding.dropdownKeterangan.text.toString()

            // Validasi input sebelum menyimpan
            if (isInputValid(
                    jenisPekerjaan,
                    pw,
                    nomorRegistrasi,
                    name,
                    address,
                    rt,
                    rw,
                    kelurahan,
                    kecamatan,
                    keterangan
                )
            ) {

                // Menampilkan loading indicator dan memblokir layar agar tidak dapat diklik
                showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                showBlockingLayer(window, true)

                // Menyimpan data pelanggan
                setValueCustomerData(
                    jenisPekerjaan,
                    pw,
                    nomorRegistrasi,
                    name,
                    address,
                    rt,
                    rw,
                    kelurahan,
                    kecamatan,
                    keterangan
                )

            } else {

                // Menampilkan pesan jika ada data yang belum diisi
                showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                showToast(this@PemasanganKelayakanActivity, R.string.fill_all_data)
            }
        } catch (e: Exception) {

            // Menangani pengecualian jika terjadi kesalahan
            e.printStackTrace()
        }
    }

    private fun setValueCustomerData(
        jenisPekerjaan: String,
        pw: String,
        nomorRegistrasi: String,
        name: String,
        address: String,
        rt: String,
        rw: String,
        kelurahan: String,
        kecamatan: String,
        keterangan: String,
    ) {

        // Bagian untuk tim petugas lapangan
        if (user?.team != 0) {

            // Mendapatkan referensi Firebase Storage
            val storageReference = FirebaseStorage.getInstance().reference

            // Mendapatkan referensi untuk file gambar dokumentasi
            val dokumentasi1Ref =
                storageReference.child("dokumentasi/${currentTime}_dokumen_${user?.username}.jpg")
            val dokumentasi2Ref =
                storageReference.child("dokumentasi/${currentTime}_kondisi_${user?.username}.jpg")


            showToast(this@PemasanganKelayakanActivity, R.string.compressing_image)

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
                                        databaseReference.child("listPemasangan").push()
                                    val newCustomerId = newCustomerRef.key

                                    if (newCustomerId != null) {

                                        // Data untuk pelanggan baru
                                        val data = mapOf(
                                            "firebaseKey" to newCustomerId,
                                            "currentDate" to currentTime,
                                            "petugas" to user?.username,
                                            "dailyTeam" to user?.dailyTeam,
                                            "jenisPekerjaan" to jenisPekerjaan,
                                            "pw" to pw.toInt(),
                                            "nomorRegistrasi" to nomorRegistrasi,
                                            "name" to name,
                                            "address" to address,
                                            "rt" to rt,
                                            "rw" to rw,
                                            "kelurahan" to kelurahan,
                                            "kecamatan" to kecamatan,
                                            "keterangan1" to keterangan,
                                            "dokumentasi1" to dokumentasi1,
                                            "dokumentasi2" to dokumentasi2,
                                            "data" to 1,
                                        )

                                        // Menyimpan data pelanggan baru ke Firebase Database
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
        }

        // Bagian untuk admin
        else {

            // Mendapatkan referensi data pelanggan yang sudah ada
            val customerRef =
                databaseReference.child("listPemasangan").child(dataCustomer!!.firebaseKey)

            // Update data pelanggan yang sudah ada di database
            val updatedValues = mapOf(
                "jenisPekerjaan" to jenisPekerjaan,
                "pw" to pw.toInt(),
                "nomorRegistrasi" to nomorRegistrasi,
                "name" to name,
                "address" to address,
                "rt" to rt,
                "rw" to rw,
                "kelurahan" to kelurahan,
                "kecamatan" to kecamatan,
                "keterangan1" to keterangan,
            )

            // Memperbarui data pelanggan yang sudah ada di Firebase Database
            customerRef.updateChildren(updatedValues).addOnCompleteListener { task ->
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
        // Jika ada perubahan, kembalikan true; jika tidak, kembalikan false.

        val newData = listOf(
            binding.dropdownJenisPekerjaan.text.toString(),
            binding.dropdownPw.text.toString(),
            binding.edtNomorRegistrasi.text.toString(),
            binding.edtNamaPelanggan.text.toString(),
            binding.edtAlamatPelanggan.text.toString(),
            binding.edtRt.text.toString(),
            binding.edtRw.text.toString(),
            binding.edtKelurahan.text.toString(),
            binding.edtKecamatan.text.toString(),
            binding.dropdownKeterangan.text.toString()
        )

        val oldData = listOf(
            dataCustomer?.jenisPekerjaan ?: "",
            dataCustomer?.pw.toString(),
            dataCustomer?.nomorRegistrasi ?: "",
            dataCustomer?.name ?: "",
            dataCustomer?.address ?: "",
            dataCustomer?.rt ?: "",
            dataCustomer?.rw ?: "",
            dataCustomer?.kelurahan ?: "",
            dataCustomer?.kecamatan ?: "",
            dataCustomer?.keterangan1 ?: ""
        )

        return newData.zip(oldData).any { (new, old) -> isDifferent(new, old) }
    }

    private fun isDifferent(newData: String, oldData: String): Boolean {

        // Fungsi ini membandingkan dua string dan mengembalikan true jika berbeda, false jika sama.
        return newData != oldData
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setCustomerData(dataCustomer: SambunganData, status: Boolean) {

        //Mengisi tampilan dengan data pelanggan yang ditemukan dari Firebase
        binding.apply {
            dropdownJenisPekerjaan.apply {
                setText(dataCustomer.jenisPekerjaan)
                if (!status) setAdapter(null)
            }.apply {
                edJenisPekerjaan.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            addedby.apply {
                text =
                    "Added by " + dataCustomer.petugas + " at " + milisToDateTime(dataCustomer.currentDate)
                visibility = android.view.View.VISIBLE
            }

            dropdownPw.apply {
                setText(dataCustomer.pw.toString())
                setAdapter(null)
            }.apply {
                edPw.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtNomorRegistrasi.setText(dataCustomer.nomorRegistrasi).apply {
                edNomorRegistrasi.apply {
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

            dropdownKeterangan.apply {
                setText(dataCustomer.keterangan1)
                setAdapter(null)
            }.apply {
                edKeterangan.apply {
                    isEnabled = status
                    isFocusable = status
                }
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

            imageView1.apply {
                Glide.with(this@PemasanganKelayakanActivity)
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

            imageView2.apply {
                Glide.with(this@PemasanganKelayakanActivity)
                    .load(dataCustomer.dokumentasi2)
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
                getString(R.string.next)
            }
        }
    }

    private fun displayData(dataCustomer: SambunganData, status: Boolean) {
        setCustomerData(dataCustomer, status)

        // Set Navigation ketika data tidak layak
        if (dataCustomer.keterangan1 == "Tidak layak") {
            binding.btnSimpan.apply {
                text = getString(R.string.finish)
                setOnClickListener {
                    navigatePage(this@PemasanganKelayakanActivity, MainActivity::class.java)
                    finish()
                }
            }
        } else {

            // Apabila data layak
            binding.btnSimpan.apply {
                isDataChanged.value = false
                if (status) setupDropdownField()
                updateButtonText()

                setOnClickListener {

                    // Cek role dari pengguna
                    // Bila admin, tampilkan dropdown dan dialog konfirmasi bila data berubah (admin = status -> true)
                    // Bila petugas lapangan, langsung lanjut ke halaman berikutnya
                    if (isDataChanged.value == true) {
                        showDataChangeDialog(this@PemasanganKelayakanActivity, ::saveCustomerData)
                        return@setOnClickListener
                    }

                    val intent = Intent(
                        this@PemasanganKelayakanActivity,
                        PemasanganSambunganActivity::class.java
                    )

                    // Mengirim kunci Firebase ke AddFirstDataActivity
                    intent.putExtra(
                        PemasanganSambunganActivity.EXTRA_USER_DATA,
                        user
                    )

                    intent.putExtra(
                        PemasanganSambunganActivity.EXTRA_CUSTOMER_DATA,
                        dataCustomer
                    )

                    startActivity(intent)
                    finish()
                }
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
                this@PemasanganKelayakanActivity,
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
                if (imageNumber == 1) {
                    firstImageFile = file
                    binding.itemImage1.text =
                        currentTime.toString() + "_dokumen.jpg"

                    // Menampilkan foto pertama di ImageView menggunakan Glide
                    Glide.with(this@PemasanganKelayakanActivity)
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
                        currentTime.toString() + "_kondisi.jpg"

                    // Menampilkan foto kedua di ImageView menggunakan Glide
                    Glide.with(this@PemasanganKelayakanActivity)
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

    companion object {
        const val EXTRA_CUSTOMER_DATA = "customer_data"
        const val EXTRA_USER_DATA = "user_data"
    }
}