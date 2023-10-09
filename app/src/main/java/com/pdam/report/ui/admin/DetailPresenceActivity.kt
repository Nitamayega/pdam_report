package com.pdam.report.ui.admin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.bumptech.glide.Glide
import com.pdam.report.MainActivity
import com.pdam.report.R
import com.pdam.report.databinding.ActivityDetailPresenceBinding

class DetailPresenceActivity : AppCompatActivity() {

    private val binding: ActivityDetailPresenceBinding by lazy {
        ActivityDetailPresenceBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val itemDate = intent.getStringExtra(EXTRA_DATE)
        val itemLocation = intent.getStringExtra(EXTRA_LOCATION)
        val itemPhotoUrl = intent.getStringExtra(EXTRA_PHOTOURL)

        binding.apply {
            tvTimestampe.text = "Diambil pada $itemDate"
            tvLocation.text = itemLocation
            Glide.with(this@DetailPresenceActivity)
                .load(itemPhotoUrl)
                .into(imgPhoto)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, AdminPresenceActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val intent = Intent(this, AdminPresenceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_PHOTOURL = "extra_photourl"
    }}