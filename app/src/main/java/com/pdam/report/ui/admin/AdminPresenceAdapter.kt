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
import com.pdam.report.data.DataPresence
import com.pdam.report.ui.officer.AddFirstDataActivity
import java.util.ArrayList

class AdminPresenceAdapter(private val presenceList: ArrayList<DataPresence>) : RecyclerView.Adapter<AdminPresenceAdapter.PresenceViewHolder>() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser = auth.currentUser
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresenceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.presence_item_row, parent, false)
        return PresenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresenceViewHolder, position: Int) {
        val presence = presenceList[position]
        holder.bind(presence)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, AddFirstDataActivity::class.java)
            intent.putExtra(DetailPresenceActivity.EXTRA_DATE, presence.currentDate)
            intent.putExtra(DetailPresenceActivity.EXTRA_LOCATION, presence.location)
            intent.putExtra(DetailPresenceActivity.EXTRA_PHOTOURL, presence.photoUrl)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return presenceList.size
    }

    inner class PresenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        private val tvTimeStampe: TextView = itemView.findViewById(R.id.tv_timestampe)
        private val imgPhoto: ImageView = itemView.findViewById(R.id.img_photo)

        fun bind(presence: DataPresence) {
            Glide.with(itemView)
                .load(presence.photoUrl)
                .into(imgPhoto)
            tvName.text = currentUser?.displayName
            tvLocation.text = presence.location
            tvTimeStampe.text = presence.currentDate

        }
    }
}