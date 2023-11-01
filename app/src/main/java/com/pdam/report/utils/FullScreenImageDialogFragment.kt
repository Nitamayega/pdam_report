package com.pdam.report.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.pdam.report.databinding.FragmentFullScreenImageDialogBinding

class FullScreenImageDialogFragment(private val imageUrl: String) : DialogFragment() {

    // View Binding
    private var _binding: FragmentFullScreenImageDialogBinding? = null
    private val binding get() = _binding!!

    // Membuat tampilan dialog dan memasang tata letak
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFullScreenImageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Konfigurasi tampilan setelah pembuatan tampilan dialog
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Menutup dialog saat tombol diklik
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        // Memuat dan menampilkan gambar menggunakan Glide
        Glide.with(this).load(imageUrl).into(binding.fullScreenImageView)

        // Mengatur dialog menjadi layar penuh
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Menutup dialog ketika mengetuk di luar dialog
        dialog?.setCanceledOnTouchOutside(true)
    }

    // Membersihkan binding saat tampilan dialog dihancurkan
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
