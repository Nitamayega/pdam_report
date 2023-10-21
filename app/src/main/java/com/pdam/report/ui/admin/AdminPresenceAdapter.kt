package com.pdam.report.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.calculateDiff
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.PresenceItemRowBinding
import com.pdam.report.utils.milisToDate
import com.pdam.report.utils.milisToDateTime

class AdminPresenceAdapter(private val presenceList: ArrayList<PresenceData>) :
    RecyclerView.Adapter<AdminPresenceAdapter.PresenceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresenceViewHolder {
        // Menginflate layout item presensi ke dalam view holder
        return PresenceViewHolder(
            PresenceItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PresenceViewHolder, position: Int) {

        // Mengikat data presensi ke tampilan item dan menambahkan aksi klik
        val presence = presenceList[position]
        holder.bind(presence)
        holder.itemView.setOnClickListener {
            // Menavigasi ke halaman detail presensi saat item diklik
            val intent = Intent(holder.itemView.context, DetailPresenceActivity::class.java)
            intent.putExtra(DetailPresenceActivity.EXTRA_DATE, presence.currentDate)
            intent.putExtra(DetailPresenceActivity.EXTRA_LOCATION, presence.location)
            intent.putExtra(DetailPresenceActivity.EXTRA_PHOTOURL, presence.photoUrl)
            intent.putExtra(DetailPresenceActivity.EXTRA_USERNAME, presence.username)
            holder.itemView.context.startActivity(intent)
        }
    }

    // Memperbarui data pada adapter dan melakukan perbandingan item yang berbeda
    fun updateData(newData: List<PresenceData>) {
        val diffResult = calculateDiff(
            PresenceDataDiffCallback(presenceList, newData)
        )
        presenceList.clear()
        presenceList.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
    }


    override fun getItemCount(): Int = presenceList.size

    inner class PresenceViewHolder(private var binding: PresenceItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(presence: PresenceData) {
            val date = milisToDate(presence.currentDate)

            binding.apply {
                Glide.with(itemView)
                    .load(presence.photoUrl)
                    .sizeMultiplier(0.5f)
                    .into(imgPhoto)
                tvName.text = presence.username
                tvLocation.text = presence.location
                tvTimestampe.text = milisToDateTime(presence.currentDate)

                // Mengelola tampilan tanggal untuk menghindari duplikasi
                if (adapterPosition > 0 && date == milisToDate(presenceList[adapterPosition - 1].currentDate)) {
                    tvDate.visibility = View.GONE
                } else {
                    tvDate.visibility = View.VISIBLE
                    tvDate.text = date
                }
            }
        }
    }

    // Kelas yang digunakan oleh DiffUtil untuk menghitung perubahan data
    class PresenceDataDiffCallback(
        private val oldList: List<PresenceData>,
        private val newList: List<PresenceData>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Memeriksa apakah item presensi pada kedua daftar adalah sama
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }


}