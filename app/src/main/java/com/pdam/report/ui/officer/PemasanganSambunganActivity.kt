package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.SambunganData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemasanganSambunganBinding
import com.pdam.report.utils.UserManager
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showDeleteConfirmationDialog
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast

class PemasanganSambunganActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPemasanganSambunganBinding.inflate(layoutInflater) }

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val firebaseKey by lazy { intent.getStringExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY) }
    private val customerData by lazy { intent.getIntExtra(PemasanganKelayakanActivity.EXTRA_CUSTOMER_DATA, 0) }

    private val userManager by lazy { UserManager() }
    private lateinit var user: UserData

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
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))

        loadDataFromFirebase(firebaseKey.toString())

        setupDropdownField()
        setupButtons()
        setUser()
    }

    private fun setupButtons() {
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
        val listCustomerRef = databaseReference.child("listPemasangan")
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
        binding.dropdownMerk.text.clear()
        binding.dropdownDiameter.text.clear()
        binding.edtStand.text.clear()
        binding.edtNomorMeter.text.clear()
        binding.edtNomorSegel.text.clear()
        binding.edtKeterangan.text.clear()
    }

    private fun saveData() {
        // Get data from input fields
        val currentDate = System.currentTimeMillis()
        val nomorKL = binding.edtNomorKl.text.toString()
        val merkMeter = binding.dropdownMerk.text.toString()
        val diameterMeter = binding.dropdownDiameter.text.toString()
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
        return nomorKL.isNotEmpty() && merk.isNotEmpty() && diameter.isNotEmpty() && stand.isNotEmpty() && nomorMeter.isNotEmpty() && nomorSegel.isNotEmpty() && keterangan.isNotEmpty()
    }

    private fun uploadImagesAndSaveData(petugas: String, currentDate: Long, nomorKL: String, merk: String, diameter: String, stand: String, nomorMeter: String, nomorSegel: String, keterangan: String) {
        saveCustomerData(
                            petugas,
                            currentDate,
                            nomorKL,
                            merk,
                            diameter,
                            stand,
                            nomorMeter,
                            nomorSegel,
                            keterangan
                        )
    }

    private fun saveCustomerData(petugas: String, currentDate: Long, nomorKL: String, merk: String, diameter: String, stand: String, nomorMeter: String, nomorSegel: String, keterangan: String) {
        val customerRef = databaseReference.child("listPemasangan").child(firebaseKey.toString())

        val data = mapOf(
            "petugas" to petugas,
            "updateInstallDate" to currentDate,
            "nomorKL" to nomorKL,
            "merkMeter" to merk,
            "diameterMeter" to diameter,
            "standMeter" to stand,
            "nomorMeter" to nomorMeter,
            "nomorSegel" to nomorSegel,
            "keterangan2" to keterangan,
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
        val customerRef = databaseReference.child("listPemasangan").child(firebaseKey)

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(SambunganData::class.java)
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

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayCustomerData(dataCustomer: SambunganData) {
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
    }

    @SuppressLint("SetTextI18n")
    private fun displayAnotherData(dataCustomer: SambunganData) {
        binding.edtNomorKl.apply {
            setText(dataCustomer.nomorKL)
            isEnabled = false
            isFocusable = false
        }

        binding.updatedby.apply {
            text = "Update by " + dataCustomer.petugas + " at " + milisToDateTime(dataCustomer.updateInstallDate)
            isEnabled = false
            isFocusable = false
            visibility = android.view.View.VISIBLE
        }

        binding.dropdownMerk.apply {
            setText(dataCustomer.merkMeter)
            isEnabled = false
            isFocusable = false
            setAdapter(null)
        }

        binding.dropdownDiameter.apply {
            setText(dataCustomer.diameterMeter)
            isEnabled = false
            isFocusable = false
            setAdapter(null)
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

        // Mengganti teks tombol Simpan untuk melanjutkan ke halaman berikutnya
        binding.btnSimpan.apply {
            if (dataCustomer.jenisPekerjaan == "Pemasangan kembali") {
                text = getString(R.string.finish)
                setOnClickListener {
                    navigatePage(this@PemasanganSambunganActivity, MainActivity::class.java, true)
                    finish()
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
                }
            }
        }
    }

    private fun setupDropdownField() {
        // Populate a dropdown field with data from resources
        val items1 = resources.getStringArray(R.array.type_of_merk)
        val items2 = resources.getStringArray(R.array.type_of_diameter)

        val dropdownField1: AutoCompleteTextView = binding.dropdownMerk
        val dropdownField2: AutoCompleteTextView = binding.dropdownDiameter

        dropdownField1.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items1))
        dropdownField2.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, items2))
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
        const val EXTRA_CUSTOMER_DATA = "customer_data"
    }
}