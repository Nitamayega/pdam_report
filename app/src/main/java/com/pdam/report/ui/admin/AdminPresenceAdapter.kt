package com.pdam.report.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.calculateDiff
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.PresenceItemRowBinding

class AdminPresenceAdapter(private val presenceList: ArrayList<PresenceData>) :
    RecyclerView.Adapter<AdminPresenceAdapter.PresenceViewHolder>() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresenceViewHolder {
        return PresenceViewHolder(
            PresenceItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PresenceViewHolder, position: Int) {
        val presence = presenceList[position]
        holder.bind(presence)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailPresenceActivity::class.java)
            intent.putExtra(DetailPresenceActivity.EXTRA_DATE, presence.currentDate)
            intent.putExtra(DetailPresenceActivity.EXTRA_LOCATION, presence.location)
            intent.putExtra(DetailPresenceActivity.EXTRA_PHOTOURL, presence.photoUrl)
            intent.putExtra(DetailPresenceActivity.EXTRA_USERNAME, presence.username)
            holder.itemView.context.startActivity(intent)
        }
    }

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
            binding.apply {
                Glide.with(itemView)
                    .load(presence.photoUrl)
                    .sizeMultiplier(0.5f)
                    .into(imgPhoto)
                tvName.text = presence.username
                tvLocation.text = presence.location
                tvTimestampe.text = presence.currentDate

                // Mengecek apakah tanggal sama dengan tanggal entri sebelumnya
                if (adapterPosition > 0 && presence.currentDate == presenceList[adapterPosition - 1].currentDate) {
                    tvDate.visibility = View.GONE
                } else {
                    tvDate.visibility = View.VISIBLE
                    tvDate.text = presence.currentDate
                }
            }
        }
    }

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
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }


}