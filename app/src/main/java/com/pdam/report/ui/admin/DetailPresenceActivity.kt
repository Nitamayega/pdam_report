package com.pdam.report.ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.pdam.report.databinding.ActivityDetailPresenceBinding
import com.pdam.report.utils.FullScreenImageDialogFragment
import com.pdam.report.utils.navigatePage

class DetailPresenceActivity : AppCompatActivity() {

    private val binding: ActivityDetailPresenceBinding by lazy {
        ActivityDetailPresenceBinding.inflate(layoutInflater)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@DetailPresenceActivity, AdminPresenceActivity::class.java)
            finish()
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Mengambil data tambahan yang dikirim melalui Intent
        val itemDate = intent.getStringExtra(EXTRA_DATE)
        val itemLocation = intent.getStringExtra(EXTRA_LOCATION)
        val itemPhotoUrl = intent.getStringExtra(EXTRA_PHOTOURL)
        val itemUsername = intent.getStringExtra(EXTRA_USERNAME)

        binding.apply {
            tvName.text = itemUsername
            tvTimestampe.text = "Diambil pada $itemDate"
            tvLocation.text = itemLocation

            Glide.with(this@DetailPresenceActivity)
                .load(itemPhotoUrl)
                .into(imgPhoto)

            imgPhoto.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(FullScreenImageDialogFragment(itemPhotoUrl!!), "FullScreenImageDialogFragment")
                    .addToBackStack(null)
                    .commit()
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

    companion object {
        // Konstanta yang digunakan untuk mengirim data tambahan melalui Intent
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_PHOTOURL = "extra_photourl"
        const val EXTRA_USERNAME = "extra_username"
    }
}