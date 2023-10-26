package com.pdam.report.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.pdam.report.databinding.FragmentFullScreenImageDialogBinding

class FullScreenImageDialogFragment(private val imageUrl: String) : DialogFragment() {

    private var _binding: FragmentFullScreenImageDialogBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenImageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        // Load and display the image using Glide
        Glide.with(this).load(imageUrl).into(binding.fullScreenImageView)

        // Make the dialog full-screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Close the dialog when tapping outside
        dialog?.setCanceledOnTouchOutside(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
