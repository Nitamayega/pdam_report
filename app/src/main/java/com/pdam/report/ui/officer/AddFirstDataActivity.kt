package com.pdam.report.ui.officer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.DataCustomer
import com.pdam.report.databinding.ActivityAddFirstDataBinding
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.showLoading
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddFirstDataActivity : AppCompatActivity() {

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser
    private val firebaseKey by lazy { intent.getStringExtra(EXTRA_FIREBASE_KEY) }

    private var imageNumber: Int = 0

    private var firstImageFile: File? = null
    private var secondImageFile: File? = null
    private val binding by lazy { ActivityAddFirstDataBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupDropdownField()
        setupButtons()
        firebaseKey?.let { loadDataFromFirebase(it) }
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener {
            imageNumber = 1
            startTakePhoto()
        }
        binding.itemImage2.setOnClickListener {
            imageNumber = 2
            startTakePhoto()
        }
        binding.btnSimpan.setOnClickListener { saveData() }
        binding.btnHapus.setOnClickListener { clearData() }
    }

    private fun clearData() {
        binding.edtPw.text.clear()
        binding.dropdownJenisPekerjaan.text.clear()
        binding.edtPw.text.clear()
        binding.edtNomorRegistrasi.text.clear()
        binding.edtNamaPelanggan.text.clear()
        binding.edtAlamatPelanggan.text.clear()
        binding.edtKeterangan.text.clear()
    }

    private fun saveData() {
        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val jenisPekerjaan = binding.dropdownJenisPekerjaan.text.toString()
        val PW = binding.edtPw.text.toString()
        val nomorRegistrasi = binding.edtNomorRegistrasi.text.toString()
        val name = binding.edtNamaPelanggan.text.toString()
        val address = binding.edtAlamatPelanggan.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        //set if edt is empty
        binding.apply {
//            dropdownJenisPekerjaan.addTextChangedListener(object : TextWatcher{
//                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                    // Nothing
//                }
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                    dropdownJenisPekerjaan.error = if (s.isNullOrEmpty()) getString(R.string.empty_field) else null
//                }
//
//                override fun afterTextChanged(p0: Editable?) {
//                    // Nothing
//                }
//            })
            dropdownJenisPekerjaan.error =
                if (jenisPekerjaan.isEmpty()) getString(R.string.empty_field) else null
            edtPw.error = if (PW == "") getString(R.string.empty_field) else null
            edtNomorRegistrasi.error =
                if (nomorRegistrasi.isEmpty()) getString(R.string.empty_field) else null
            edtNamaPelanggan.error = if (name.isEmpty()) getString(R.string.empty_field) else null
            edtAlamatPelanggan.error =
                if (address.isEmpty()) getString(R.string.empty_field) else null
            edtKeterangan.error =
                if (keterangan.isEmpty()) getString(R.string.empty_field) else null
        }

        showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)

        if (jenisPekerjaan.isNotEmpty() && PW != "" && nomorRegistrasi.isNotEmpty() && name.isNotEmpty() && address.isNotEmpty() && keterangan.isNotEmpty() && (firstImageFile != null) && (secondImageFile != null)
        ) {
            Log.d("Jenis Pekerjaan", jenisPekerjaan)
            val storageReference = FirebaseStorage.getInstance().reference

            val dokumentasi1Ref =
                storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi1.jpg")
            val dokumentasi2Ref =
                storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi2.jpg")

            // Upload file ke Firebase Storage
            val uploadTask1 = dokumentasi1Ref.putFile(Uri.fromFile(firstImageFile))
            val uploadTask2 = dokumentasi2Ref.putFile(Uri.fromFile(secondImageFile))

            uploadTask1.addOnSuccessListener {
                dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                    val dokumentasi1 = uri1.toString()

                    uploadTask2.addOnSuccessListener {
                        dokumentasi2Ref.downloadUrl.addOnSuccessListener { uri2 ->
                            val dokumentasi2 = uri2.toString()

                            // Setelah berhasil mendapatkan URL, simpan data ke Firebase Realtime Database
                            val userReference = currentUser?.let { databaseReference.child("users").child(it.uid) }
                            val newCustomerRef =
                                userReference?.child("listCustomer")?.push() // Membuat simpul baru
                            val newCustomerId = newCustomerRef?.key // Mengambil ID dari simpul baru

                            if (newCustomerId != null) {
                                val data = DataCustomer(
                                    firebaseKey = newCustomerId, // Menggunakan ID sebagai firebaseKey
                                    currentDate = currentDate,
                                    jenisPekerjaan = jenisPekerjaan,
                                    pw = PW.toInt(),
                                    nomorRegistrasi = nomorRegistrasi,
                                    name = name,
                                    address = address,
                                    keterangan = keterangan,
                                    dokumentasi1 = dokumentasi1,
                                    dokumentasi2 = dokumentasi2
                                )

                                newCustomerRef.setValue(data).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("Jenis Pekerjaan save", jenisPekerjaan)
                                        showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                                        Toast.makeText(
                                            this,
                                            "Data berhasil disimpan",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                                        finish()
                                    } else {
                                        showLoading(true, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                                        Toast.makeText(
                                            this,
                                            "Data gagal disimpan",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Handle jika foto belum diambil atau data belum lengkap
            showLoading(false, binding.progressBar, binding.btnSimpan, binding.btnHapus)
            Toast.makeText(this, "Harap isi semua data dan ambil kedua foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataFromFirebase(firebaseKey: String) {
        val userReference = currentUser?.let { databaseReference.child("users").child(it.uid) }

        // Gunakan kunci Firebase untuk mengambil data dari Firebase Realtime Database
        userReference?.child("listCustomer")?.child(firebaseKey)?.addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataCustomer = snapshot.getValue(DataCustomer::class.java)
                    if (dataCustomer != null) {
                        binding.dropdownJenisPekerjaan.apply {
                            setText(dataCustomer.jenisPekerjaan, false)
                            isEnabled = false
                            isFocusable = false
                        }

                        binding.edtPw.apply {
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

                        binding.edtKeterangan.apply {
                            setText(dataCustomer.keterangan)
                            isEnabled = false
                            isFocusable = false
                        }

                        val typeOfWorkArray = resources.getStringArray(R.array.type_of_work)
                        binding.btnSimpan.apply {
                            text = context.getString(R.string.next)
                            setOnClickListener {
                                when (dataCustomer.jenisPekerjaan) {
                                    typeOfWorkArray[0] -> {
                                        val intent = Intent(context, UpdateCustomerInstallationActivity::class.java)
                                        intent.putExtra(EXTRA_FIREBASE_KEY, firebaseKey)
                                        context.startActivity(intent)
                                    }
                                    typeOfWorkArray[1] -> {
                                        val intent = Intent(context, UpdateCustomerVerificationActivity::class.java)
                                        intent.putExtra(EXTRA_FIREBASE_KEY, firebaseKey)
                                        context.startActivity(intent)
                                    }
                                    else -> {
                                        Toast.makeText(context, "Jenis pekerjaan tidak ditemukan", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(context, MainActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        }

                    }
                } else {
                    // Data tidak ditemukan
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Penanganan kesalahan saat mengambil data dari Firebase
                // Misalnya, menampilkan pesan kesalahan kepada pengguna
            }
        })
    }



    private lateinit var currentPhotoPath: String
    private fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also { file ->
            val photoURI: Uri = FileProvider.getUriForFile(
                this@AddFirstDataActivity,
                "com.pdam.report",
                file
            )
            currentPhotoPath = file.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            launcherIntentCamera.launch(intent) // Mengirimkan nomor gambar sebagai request code
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                if (imageNumber == 1) {
                    firstImageFile = file
                    binding.itemImage1.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                } else if (imageNumber == 2) {
                    secondImageFile = file
                    binding.itemImage2.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        super.onBackPressed()
    }

    private fun setupDropdownField() {
        val items = resources.getStringArray(R.array.type_of_work)
        val dropdownField: AutoCompleteTextView = binding.dropdownJenisPekerjaan
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, items)
        dropdownField.setAdapter(adapter)
    }

    companion object {
        const val EXTRA_FIREBASE_KEY = "firebase_key"
    }
}