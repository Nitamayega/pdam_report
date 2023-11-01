package com.pdam.report.ui.officer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.SambunganData
import com.pdam.report.data.UserData
import com.pdam.report.databinding.ActivityPemasanganSambunganBinding
import com.pdam.report.utils.getNetworkTime
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import com.pdam.report.utils.showBlockingLayer
import com.pdam.report.utils.showDataChangeDialog
import com.pdam.report.utils.showDeleteConfirmationDialog
import com.pdam.report.utils.showLoading
import com.pdam.report.utils.showToast
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class PemasanganSambunganActivity : AppCompatActivity() {

    // View Binding
    private val binding by lazy { ActivityPemasanganSambunganBinding.inflate(layoutInflater) }

    // Firebase Database
    private val databaseReference = FirebaseDatabase.getInstance().reference

    // Waktu saat ini didapat dari server
    private var currentTime by Delegates.notNull<Long>()

    // Intent-related: Data customer
    private val dataCustomer by lazy {
        intent.getParcelableExtra<SambunganData>(
            PemasanganKelayakanActivity.EXTRA_CUSTOMER_DATA
        )
    }

    // Intent-related: User (petugas dan/atau admin)
    private val user by lazy {
        intent.getParcelableExtra<UserData>(
            PemasanganKelayakanActivity.EXTRA_USER_DATA
        )
    }

    // Memantau perubahan di semua field yang relevan
    private val isDataChanged = MutableLiveData<Boolean>()

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur tampilan dan tombol back
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this@PemasanganSambunganActivity, onBackPressedCallback)

        // Mengatur style action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))
        }

        // Melakukan operasi asynchronous untuk mendapatkan waktu saat ini dari server
        lifecycleScope.launch {
            currentTime = getNetworkTime()

            // Persiapan dropdown, tombol, dan tampilan
            setupDropdownField()
            monitorDataChanges()
            setupButtons()

            // Menampilkan data pelanggan
            displayCustomerData()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            // Menangani tombol back: Navigasi ke PemasanganSambungan dan menambahkan firebaseKey sebagai ekstra dalam Intent
            val intent =
                Intent(this@PemasanganSambunganActivity, PemasanganKelayakanActivity::class.java)
            intent.putExtra(PemasanganKelayakanActivity.EXTRA_CUSTOMER_DATA, dataCustomer)
            intent.putExtra(PemasanganKelayakanActivity.EXTRA_USER_DATA, user)
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

            // Memeriksa apakah pengguna adalah petugas dan status data pelanggan "Layak"
            val setRole = user?.team == 0
            setCustomerData(dataCustomer!!, setRole)

            if (dataCustomer?.data != 1) {
                displayData(dataCustomer!!, setRole)
            }
        }
    }

    private fun setupButtons() {

        // Menetapkan tindakan yang dilakukan saat tombol "Simpan" diklik
        binding.btnSimpan.setOnClickListener { saveCustomerData() }

        // Menetapkan tindakan yang dilakukan saat tombol "Hapus" diklik
        binding.btnHapus.setOnClickListener {
            if (dataCustomer?.data == 1) {
                clearData()
            } else {
                deleteData()
            }
        }
    }

    private fun setupDropdownField() {

        // Mengambil array data dari resources untuk setiap dropdown
        val items1 = resources.getStringArray(R.array.type_of_merk)
        val items2 = resources.getStringArray(R.array.type_of_diameter)

        // Mendefinisikan AutoCompleteTextView untuk setiap dropdown
        val dropdownField1 = binding.dropdownMerk
        val dropdownField2 = binding.dropdownDiameter

        // Membuat adapter ArrayAdapter<String> untuk setiap dropdown dengan menggunakan layout default
        val adapter1 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items1)
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items2)

        // Mengatur adapter untuk setiap dropdown
        dropdownField1.setAdapter(adapter1)
        dropdownField2.setAdapter(adapter2)
    }

    private fun clearData() {

        // Membersihkan semua isian pada field input
        binding.apply {
            edtNomorKl.text.clear()
            dropdownMerk.text.clear()
            dropdownDiameter.text.clear()
            edtStand.text.clear()
            edtNomorMeter.text.clear()
            edtNomorSegel.text.clear()
            edtKeterangan.text.clear()
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
                    showDeleteConfirmationDialog(customerRef, this@PemasanganSambunganActivity)
                } else {

                    // Jika data tidak ditemukan, tampilkan pesan bahwa data tidak ditemukan
                    showToast(this@PemasanganSambunganActivity, R.string.data_not_found)
                }
            }

            override fun onCancelled(error: DatabaseError) {

                // Jika terjadi kesalahan saat mengakses data, tampilkan pesan kesalahan
                showToast(
                    this@PemasanganSambunganActivity,
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
            edtPemasanganSambungan.addTextChangedListener(textWatcher)
            edtPw.addTextChangedListener(textWatcher)
            edtNomorKl.addTextChangedListener(textWatcher)
            edtNamaPelanggan.addTextChangedListener(textWatcher)
            edtAlamatPelanggan.addTextChangedListener(textWatcher)
            edtRt.addTextChangedListener(textWatcher)
            edtRw.addTextChangedListener(textWatcher)
            edtKelurahan.addTextChangedListener(textWatcher)
            edtKecamatan.addTextChangedListener(textWatcher)
            dropdownMerk.addTextChangedListener(textWatcher)
            dropdownDiameter.addTextChangedListener(textWatcher)
            edtStand.addTextChangedListener(textWatcher)
            edtNomorMeter.addTextChangedListener(textWatcher)
            edtNomorSegel.addTextChangedListener(textWatcher)
            edtKeterangan.addTextChangedListener(textWatcher)
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
        merk: String,
        diameter: String,
        stand: String,
        nomorMeter: String,
        nomorSegel: String,
        keterangan: String,
    ): Boolean {

        // Return true jika semua validasi terpenuhi
        return jenisPekerjaan.isNotEmpty() && pw.isNotEmpty() && nomorKL.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && rt.isNotEmpty() && rw.isNotEmpty() && kelurahan.isNotEmpty() && kecamatan.isNotEmpty() && merk.isNotEmpty() && diameter.isNotEmpty() && stand.isNotEmpty() && nomorMeter.isNotEmpty() && nomorSegel.isNotEmpty() && keterangan.isNotEmpty()
    }

    private fun saveCustomerData() {
        val jenisPekerjaan = binding.edtPemasanganSambungan.text.toString()
        val pw = binding.edtPw.text.toString()
        val nomorKL = binding.edtNomorKl.text.toString()
        val name = binding.edtNamaPelanggan.text.toString()
        val address = binding.edtAlamatPelanggan.text.toString()
        val rt = binding.edtRt.text.toString()
        val rw = binding.edtRw.text.toString()
        val kelurahan = binding.edtKelurahan.text.toString()
        val kecamatan = binding.edtKecamatan.text.toString()
        val merkMeter = binding.dropdownMerk.text.toString()
        val diameterMeter = binding.dropdownDiameter.text.toString()
        val standMeter = binding.edtStand.text.toString()
        val nomorMeter = binding.edtNomorMeter.text.toString()
        val nomorSegel = binding.edtNomorSegel.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        // Validasi input sebelum menyimpan
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
                merkMeter,
                diameterMeter,
                standMeter,
                nomorMeter,
                nomorSegel,
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
                nomorKL,
                name,
                address,
                rt,
                rw,
                kelurahan,
                kecamatan,
                merkMeter,
                diameterMeter,
                standMeter,
                nomorMeter,
                nomorSegel,
                keterangan
            )
        } else {

            // Menampilkan pesan jika ada data yang belum diisi
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            showToast(this, R.string.fill_all_data)
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
        merk: String,
        diameter: String,
        stand: String,
        nomorMeter: String,
        nomorSegel: String,
        keterangan: String,
    ) {

        // Inisialisasi variabel untuk data pelanggan
        val customerRef =
            databaseReference.child("listPemasangan").child(dataCustomer!!.firebaseKey)

        // Update data pelanggan yang sudah ada
        val updatedValues = mapOf(
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
            "merkMeter" to merk,
            "diameterMeter" to diameter,
            "standMeter" to stand,
            "nomorMeter" to nomorMeter,
            "nomorSegel" to nomorSegel,
            "keterangan2" to keterangan,
            "data" to 2
        )

        // Memperbarui data pelanggan yang sudah ada di database
        customerRef.updateChildren(updatedValues).addOnCompleteListener { task ->
            handleSaveCompletionOrFailure(task)
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
            binding.edtPemasanganSambungan.text.toString(),
            binding.edtPw.text.toString(),
            binding.edtNomorKl.text.toString(),
            binding.edtNamaPelanggan.text.toString(),
            binding.edtAlamatPelanggan.text.toString(),
            binding.edtRt.text.toString(),
            binding.edtRw.text.toString(),
            binding.edtKelurahan.text.toString(),
            binding.edtKecamatan.text.toString(),
            binding.dropdownMerk.text.toString(),
            binding.dropdownDiameter.text.toString(),
            binding.edtStand.text.toString(),
            binding.edtNomorMeter.text.toString(),
            binding.edtNomorSegel.text.toString(),
            binding.edtKeterangan.text.toString()
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
            dataCustomer?.merkMeter.toString(),
            dataCustomer?.diameterMeter.toString(),
            dataCustomer?.standMeter.toString(),
            dataCustomer?.nomorMeter.toString(),
            dataCustomer?.nomorSegel.toString(),
            dataCustomer?.keterangan2.toString()
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
            edtPemasanganSambungan.setText(dataCustomer.jenisPekerjaan).apply {
                edPemasanganSambungan.apply {
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
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAnotherCustomerData(dataCustomer: SambunganData, status: Boolean) {

        // Mengisi tampilan dengan data lainnya
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

            dropdownMerk.apply {
                setText(dataCustomer.merkMeter)
                setAdapter(null)
            }.apply {
                edMerk.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            dropdownDiameter.apply {
                setText(dataCustomer.diameterMeter)
                setAdapter(null)
            }.apply {
                edDiameter.apply {
                    isEnabled = status
                    isFocusable = status
                }
            }

            edtStand.setText(dataCustomer.standMeter).apply {
                edStand.apply {
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

            edtKeterangan.setText(dataCustomer.keterangan2).apply {
                edKeterangan.apply {
                    isEnabled = status
                    isFocusable = status
                }
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
        setAnotherCustomerData(dataCustomer, status)

        if (dataCustomer.jenisPekerjaan == "Pemasangan kembali") {
            binding.btnSimpan.apply {
                text = getString(R.string.finish)
                setOnClickListener {
                    navigatePage(
                        this@PemasanganSambunganActivity,
                        MainActivity::class.java,
                        true
                    )
                    finish()
                }
            }
        } else {
            binding.btnSimpan.apply {
                isDataChanged.value = false
                if (status) {
                    setupDropdownField()
                }
                updateButtonText()

                setOnClickListener {
                    // Cek role dari pengguna
                    // Bila admin, tampilkan dropdown dan dialog konfirmasi bila data berubah (admin = status -> true)
                    // Bila petugas lapangan, langsung lanjut ke halaman berikutnya

                    if (isDataChanged.value == true) {
                        showDataChangeDialog(
                            this@PemasanganSambunganActivity,
                            this@PemasanganSambunganActivity::saveCustomerData
                        )
                        return@setOnClickListener
                    }

                    val intent = Intent(
                        this@PemasanganSambunganActivity,
                        PemasanganGPSActivity::class.java
                    )

                    intent.putExtra(
                        PemasanganGPSActivity.EXTRA_USER_DATA,
                        user
                    )

                    intent.putExtra(
                        PemasanganGPSActivity.EXTRA_CUSTOMER_DATA,
                        dataCustomer
                    )

                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_CUSTOMER_DATA = "customer_data"
        const val EXTRA_USER_DATA = "user_data"
    }
}