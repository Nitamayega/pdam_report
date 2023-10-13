package com.pdam.report.ui.officer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.data.DataCustomer
import com.pdam.report.databinding.ActivityUpdateCustomerInstallationBinding

class UpdateCustomerInstallationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUpdateCustomerInstallationBinding.inflate(layoutInflater) }

    private val databaseReference = FirebaseDatabase.getInstance().reference

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser
    private val firebaseKey by lazy { intent.getStringExtra(AddFirstDataActivity.EXTRA_FIREBASE_KEY) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        firebaseKey?.let { loadDataFromFirebase(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, AddFirstDataActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, AddFirstDataActivity::class.java)
        startActivity(intent)
        super.onBackPressed()
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
                        binding.edtPemasanganSambungan.apply {
                            setText(dataCustomer.jenisPekerjaan.toString())
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

                        binding.edtKeterangan.apply {
                            setText(dataCustomer.keterangan)
                        }

                        binding.itemImage.apply {
                            text = dataCustomer.dokumentasi2
                            isEnabled = false
                        }

                        val typeOfWorkArray = resources.getStringArray(R.array.type_of_work)
                        binding.btnSimpan.apply {
                            text = context.getString(R.string.next)
                            setOnClickListener {
                                when (dataCustomer.jenisPekerjaan) {
                                    typeOfWorkArray[0] -> {
                                        val intent = Intent(context, UpdateCustomerInstallationActivity::class.java)
                                        intent.putExtra(AddFirstDataActivity.EXTRA_FIREBASE_KEY, firebaseKey)
                                        context.startActivity(intent)
                                    }
                                    typeOfWorkArray[1] -> {
                                        val intent = Intent(context, UpdateCustomerVerificationActivity::class.java)
                                        intent.putExtra(AddFirstDataActivity.EXTRA_FIREBASE_KEY, firebaseKey)
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
}