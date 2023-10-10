package com.pdam.report.ui.officer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.databinding.ActivityAddFirstDataBinding
import com.pdam.report.utils.createCustomTempFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddFirstDataActivity : AppCompatActivity() {

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
        binding.itemImage1.setOnClickListener { startTakePhoto(2) }
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