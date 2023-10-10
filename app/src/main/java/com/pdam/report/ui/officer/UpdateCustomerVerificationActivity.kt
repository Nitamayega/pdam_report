package com.pdam.report.ui.officer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pdam.report.databinding.ActivityUpdateCustomerVerificationBinding

class UpdateCustomerVerificationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityUpdateCustomerVerificationBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}