package com.pdam.report.ui.officer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.DataCustomer
import com.pdam.report.databinding.ActivityAddFirstDataBinding
import com.pdam.report.utils.createCustomTempFile
import com.pdam.report.utils.reduceFileImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddFirstDataActivity : AppCompatActivity() {

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

    private var getFile: File? = null
    private val binding by lazy { ActivityAddFirstDataBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupDropdownField()
        setupButtons()
    }

    private fun setupButtons() {
        binding.itemImage1.setOnClickListener { startTakePhoto(1) }
        binding.itemImage2.setOnClickListener { startTakePhoto(2) }
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
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val jenisPekerjaan = binding.dropdownJenisPekerjaan.text.toString()
        val PW = binding.edtPw.text.toString().toInt()
        val nomorRegistrasi = binding.edtNomorRegistrasi.text.toString()
        val name = binding.edtNamaPelanggan.text.toString()
        val address = binding.edtAlamatPelanggan.text.toString()
        val keterangan = binding.edtKeterangan.text.toString()

        // Pastikan getFile tidak null
        if (getFile != null) {
            val userReference = currentUser?.let { databaseReference.child("users").child(it.uid) }
            val storageReference = FirebaseStorage.getInstance().reference

            val dokumentasi1Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi1.jpg")
            val dokumentasi2Ref = storageReference.child("dokumentasi/${System.currentTimeMillis()}_dokumentasi2.jpg")

            // Upload file ke Firebase Storage
            val uploadTask1 = dokumentasi1Ref.putFile(Uri.fromFile(getFile))
            val uploadTask2 = dokumentasi2Ref.putFile(Uri.fromFile(getFile))

            uploadTask1.addOnSuccessListener { taskSnapshot1 ->
                dokumentasi1Ref.downloadUrl.addOnSuccessListener { uri1 ->
                    val dokumentasi1 = uri1.toString()

                    uploadTask2.addOnSuccessListener { taskSnapshot2 ->
                        dokumentasi2Ref.downloadUrl.addOnSuccessListener { uri2 ->
                            val dokumentasi2 = uri2.toString()

                            // Setelah berhasil mendapatkan URL, simpan data ke Firebase Realtime Database
                            val data = DataCustomer(
                                currentDate = currentDate,
                                jenisPekerjaan = jenisPekerjaan,
                                PW = PW,
                                nomorRegistrasi = nomorRegistrasi,
                                name = name,
                                address = address,
                                keterangan = keterangan,
                                dokumentasi1 = dokumentasi1,
                                dokumentasi2 = dokumentasi2
                            )

                            showLoading(true)
                            userReference?.child("listCustomer")?.push()?.setValue(data)?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    showLoading(false)
                                    Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                                    showLoading(false)
                                    finish()
                                } else {
                                    showLoading(true)
                                    Toast.makeText(this, "Data gagal disimpan", Toast.LENGTH_SHORT).show()
                                    showLoading(false)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Handle jika getFile null
            Toast.makeText(this, "Foto belum diambil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !isLoading
        binding.btnHapus.isEnabled = !isLoading
    }

    private lateinit var currentPhotoPath: String
    private fun startTakePhoto(imageNumber: Int) {
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
            launcherIntentCamera(imageNumber).launch(intent)
        }
    }

    private fun launcherIntentCamera(imageNumber: Int) = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)
            myFile.let { file ->
                getFile = file
                val textViewToUpdate = when (imageNumber) {
                    1 -> binding.itemImage1
                    2 -> binding.itemImage2
                    else -> null
                }
                textViewToUpdate?.text =
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
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
}