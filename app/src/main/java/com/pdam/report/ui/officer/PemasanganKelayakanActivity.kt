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
import com.pdam.report.data.CustomerData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemasanganKelayakanBinding
import com.pdam.report.utils.FullScreenImageDialogFragment
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.parsingNameImage
import com.pdam.report.utils.reduceFileImageInBackground
import com.pdam.report.utils.showDeleteConfirmationDialog
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import kotlinx.coroutines.launch
import java.io.File

class PemasanganKelayakanActivity : AppCompatActivity() {

    private val databaseReference = FirebaseDatabase.getInstance().reference


    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

    private val firebaseKey by lazy {
        intent.getStringExtra(EXTRA_FIREBASE_KEY)
    }

    private var imageNumber: Int = 0

    private var firstImageFile: File? = null
    private var secondImageFile: File? = null
    private val binding by lazy { ActivityPemasanganKelayakanBinding.inflate(layoutInflater) }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@PemasanganKelayakanActivity, MainActivity::class.java)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupDropdownField()
        setupButtons()
        setUser()
        firebaseKey?.let { loadDataFromFirebase(it) }
        Log.d("firebaseKeyOnAddFirst", firebaseKey.toString())
    }

    private fun setUser() {
        userManager.fetchUserAndSetupData {
            user = userManager.getUser()
        }
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener { imageNumber = 1; startTakePhoto() }
        binding.itemImage2.setOnClickListener { imageNumber = 2; startTakePhoto() }

        binding.btnSimpan.setOnClickListener { saveData() }

        binding.btnHapus.setOnClickListener {
            if (firebaseKey == null) {
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
                    showDeleteConfirmationDialog(customerRef, this@PemasanganKelayakanActivity)
                } else {
                    showToast(this@PemasanganKelayakanActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(this@PemasanganKelayakanActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    private fun clearData() {
        // Clear all input fields
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
        }
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
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

        // Validate input
        if (isInputValid(jenisPekerjaan, pw, nomorRegistrasi, name, address, rt, rw, kelurahan, kecamatan, keterangan)) {
            showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            uploadImagesAndSaveData(currentDate, jenisPekerjaan, pw, nomorRegistrasi, name, address, rt, rw, kelurahan, kecamatan, keterangan)
        } else {
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_dataImage)
        }
    }

    private fun isInputValid(jenisPekerjaan: String, pw: String, nomorRegistrasi: String, name: String, address: String, rt: String, rw: String, kelurahan: String, kecamatan: String, keterangan: String): Boolean {
        // Check if all required input is valid
        return jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorRegistrasi.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && keterangan.isNotEmpty() && (firstImageFile != null) && (secondImageFile != null)
    }

    private fun uploadImagesAndSaveData(currentDate: Long, jenisPekerjaan: String, pw: String, nomorRegistrasi: String, name: String, address: String, rt: String, rw: String, kelurahan: String, kecamatan: String, keterangan: String) {
        val storageReference = FirebaseStorage.getInstance().reference
        val dokumentasi1Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi1_dokumen.jpg")
        val dokumentasi2Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi2_kondisi.jpg")

        lifecycleScope.launch {
            firstImageFile = firstImageFile?.reduceFileImageInBackground()
            secondImageFile = secondImageFile?.reduceFileImageInBackground()
        }

        // Upload image 1
        dokumentasi1Ref.putFile(Uri.fromFile(firstImageFile)).addOnSuccessListener {
            dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                val dokumentasi1 = uri1.toString()

                // Upload image 2
                dokumentasi2Ref.putFile(Uri.fromFile(secondImageFile)).addOnSuccessListener {
                    dokumentasi2Ref.downloadUrl.addOnSuccessListener { uri2 ->
                        val dokumentasi2 = uri2.toString()

                        // After successfully obtaining image URLs, save the data to Firebase
                        saveCustomerData(currentDate, jenisPekerjaan, pw, nomorRegistrasi, name, address, rt, rw, kelurahan, kecamatan, keterangan, dokumentasi1, dokumentasi2)
                    }
                }
            }
        }
    }

    private fun saveCustomerData(currentDate: Long, jenisPekerjaan: String, pw: String, nomorRegistrasi: String, name: String, address: String, rt: String, rw: String, kelurahan: String, kecamatan: String, keterangan: String, dokumentasi1: String, dokumentasi2: String) {
        val newCustomerRef = databaseReference.child("listCustomer").push()
        val newCustomerId = newCustomerRef.key


        if (newCustomerId != null) {
            val data = CustomerData(
                firebaseKey = newCustomerId,
                currentDate = currentDate,
                petugas =  user.username,
                jenisPekerjaan = jenisPekerjaan,
                pw = pw.toInt(),
                nomorRegistrasi = nomorRegistrasi,
                name = name,
                address = address,
                rt = rt,
                rw = rw,
                kelurahan = kelurahan,
                kecamatan = kecamatan,
                keterangan1 = keterangan,
                dokumentasi1 = dokumentasi1,
                dokumentasi2 = dokumentasi2,
                data = 1,
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
        val customerRef = databaseReference.child("listCustomer").child(firebaseKey)

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(CustomerData::class.java)
                    if (dataCustomer != null) {
                        // Jika data pelanggan ditemukan, tampilkan datanya
                        displayCustomerData(dataCustomer)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan kesalahan jika mengakses data gagal
                showToast(this@PemasanganKelayakanActivity, "${R.string.failed_access_data}: ${error.message}".toInt())
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun displayCustomerData(dataCustomer: CustomerData) {
        // Mengisi tampilan dengan data pelanggan yang ditemukan dari Firebase
        binding.dropdownJenisPekerjaan.apply {
            setText(dataCustomer.jenisPekerjaan)
            isEnabled = false
            isFocusable = false
        }

        binding.addedby.apply {
            text = "Added by " + dataCustomer.petugas + " at " + milisToDateTime(dataCustomer.currentDate)
            isEnabled = false
            isFocusable = false
            visibility = android.view.View.VISIBLE
        }

            binding.dropdownPw.apply {
                setText(dataCustomer.pw.toString())
                isEnabled = false
                isFocusable = false
            }


            binding.edtNomorRegistrasi.apply {
                setText(dataCustomer.nomorRegistrasi)
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

            binding.dropdownKeterangan.apply {
                setText(dataCustomer.keterangan1)
                isEnabled = false
                isFocusable = false
            }

            binding.itemImage1.apply {
                text = parsingNameImage(dataCustomer.dokumentasi1)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi1), "FullScreenImageDialogFragment")
                        .addToBackStack(null)
                        .commit()
                }
            }

            binding.itemImage2.apply {
                text = parsingNameImage(dataCustomer.dokumentasi2)
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .add(FullScreenImageDialogFragment(dataCustomer.dokumentasi2), "FullScreenImageDialogFragment")
                        .addToBackStack(null)
                        .commit()
                }
            }


        if (dataCustomer.keterangan1 == "Tidak Layak") {
            binding.btnSimpan.text = getString(R.string.finish)
            binding.btnSimpan.setOnClickListener {
                navigatePage(this@PemasanganKelayakanActivity, MainActivity::class.java, true)
                finish()
            }
        } else {


        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            binding.btnSimpan.text = getString(R.string.next)
            binding.btnSimpan.setOnClickListener {
                val intent = Intent(
                    this@PemasanganKelayakanActivity,
                    PemasanganSambunganActivity::class.java
                )

                // Mengirim kunci Firebase ke AddFirstDataActivity
                intent.putExtra(
                    PemasanganSambunganActivity.EXTRA_FIREBASE_KEY,
                    dataCustomer.firebaseKey
                )
                intent.putExtra(
                    PemasanganSambunganActivity.EXTRA_CUSTOMER_DATA,
                    dataCustomer.data
                )

                startActivity(intent)
                finish()
            }
        }
        }
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@PemasanganKelayakanActivity,
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
                    binding.itemImage1.text = System.currentTimeMillis().toString() + "_dokumen.jpg"

                    Glide.with(this@PemasanganKelayakanActivity)
                        .load(firstImageFile)
                        .into(binding.imageView1)

                    binding.imageView1.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(FullScreenImageDialogFragment(firstImageFile.toString()), "FullScreenImageDialogFragment")
                            .addToBackStack(null)
                            .commit()
                    }

                } else if (imageNumber == 2) {
                    secondImageFile = file
                    binding.itemImage2.text = System.currentTimeMillis().toString() + "_kondisi.jpg"

                    Glide.with(this@PemasanganKelayakanActivity)
                        .load(secondImageFile)
                        .into(binding.imageView2)

                    binding.imageView2.setOnClickListener {
                        supportFragmentManager.beginTransaction()
                            .add(FullScreenImageDialogFragment(secondImageFile.toString()), "FullScreenImageDialogFragment")
                            .addToBackStack(null)
                            .commit()
                    }
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

    private fun setupDropdownField() {
        // Populate a dropdown field with data from resources
        val items1 = resources.getStringArray(R.array.type_of_work)
        val items2 = resources.getStringArray(R.array.type_of_pw)
        val items3 = resources.getStringArray(R.array.type_of_ket)


        val dropdownField1: AutoCompleteTextView = binding.dropdownJenisPekerjaan
        val dropdownField2: AutoCompleteTextView = binding.dropdownPw
        val dropdownField3: AutoCompleteTextView = binding.dropdownKeterangan

        dropdownField1.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items1))
        dropdownField2.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items2))
        dropdownField3.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items3))
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}