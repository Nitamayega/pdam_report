package com.pdam.report.ui.officer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pdam.report.R
import com.pdam.report.databinding.ActivityUpdateCustomerInstallationBinding

class UpdateCustomerInstallationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUpdateCustomerInstallationBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}