package com.pdam.report.ui.officer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pdam.report.R
import com.pdam.report.databinding.ActivityUpdateCustomerVerificationBinding

class UpdateCustomerVerificationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUpdateCustomerVerificationBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}