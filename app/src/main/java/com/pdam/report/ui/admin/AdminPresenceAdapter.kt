package com.pdam.report.ui.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.pdam.report.R
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.PresenceItemRowBinding
import com.pdam.report.ui.officer.AddFirstDataActivity
import java.util.ArrayList

class AdminPresenceAdapter(private val presenceList: ArrayList<PresenceData>) : RecyclerView.Adapter<AdminPresenceAdapter.PresenceViewHolder>() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser

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
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return presenceList.size
    }

    inner class PresenceViewHolder(private var binding: PresenceItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(presence: PresenceData) {
            binding.apply {
                Glide.with(itemView)
                    .load(presence.photoUrl)
                    .into(imgPhoto)
                tvName.text = presence.username
                tvLocation.text = presence.location
                tvTimestampe.text = presence.currentDate
            }
        }
    }
}