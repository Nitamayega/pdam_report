package com.pdam.report

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pdam.report.data.SambunganData
import com.pdam.report.databinding.ReportItemRowBinding
import com.pdam.report.ui.officer.PemasanganKelayakanActivity
import com.pdam.report.ui.officer.PemutusanActivity

class MainAdapter(
    private val customerList: ArrayList<SambunganData>,
    private val fragmentType: Int,
) :
    RecyclerView.Adapter<MainAdapter.CustomerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        return CustomerViewHolder(
            ReportItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customerList[position]
        holder.bind(customer)
        holder.itemView.setOnClickListener {
            val intent = Intent(
                holder.itemView.context,
                if (fragmentType == 0) {
                    PemasanganKelayakanActivity::class.java
                } else {
                    PemutusanActivity::class.java
                }
            )

            intent.putExtra(PemasanganKelayakanActivity.EXTRA_FIREBASE_KEY, customer.firebaseKey)
            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateData(newData: List<SambunganData>) {
        val diffResult = DiffUtil.calculateDiff(
            CustomerDataDiffCallback(customerList, newData)
        )
        customerList.clear()
        customerList.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
    }


    override fun getItemCount(): Int = customerList.size

    inner class CustomerViewHolder(private var binding: ReportItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: SambunganData) {
            binding.apply {
                tvName.text = customer.name
                tvAddress.text = customer.address
                tvPetugas.text = customer.petugas
            }
        }
    }

    class CustomerDataDiffCallback(
        private val oldList: List<SambunganData>,
        private val newList: List<SambunganData>,
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