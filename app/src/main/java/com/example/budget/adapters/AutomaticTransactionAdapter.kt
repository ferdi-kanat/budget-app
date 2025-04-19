package com.example.budget.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import com.example.budget.data.AutomaticTransaction
import com.example.budget.data.RepeatPeriod
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class AutomaticTransactionAdapter(
    private val onDeleteClick: (AutomaticTransaction) -> Unit,
    private val onActiveStateChanged: (AutomaticTransaction, Boolean) -> Unit
) : ListAdapter<AutomaticTransaction, AutomaticTransactionAdapter.ViewHolder>(AutomaticTransactionDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_automatic_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val repeatPeriodTextView: TextView = itemView.findViewById(R.id.repeatPeriodTextView)
        private val activeSwitch: SwitchMaterial = itemView.findViewById(R.id.activeSwitch)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(transaction: AutomaticTransaction) {
            amountTextView.text = String.format("%.2f TL", transaction.amount)
            descriptionTextView.text = transaction.description
            dateTextView.text = dateFormatter.format(Date(transaction.paymentDate))
            repeatPeriodTextView.text = when (transaction.repeatPeriod) {
                RepeatPeriod.DAILY -> "Her gün"
                RepeatPeriod.WEEKLY -> "Her hafta"
                RepeatPeriod.MONTHLY -> "Her ay"
                RepeatPeriod.YEARLY -> "Her yıl"
                else -> "Bilinmeyen periyot"
            }
            
            activeSwitch.isChecked = transaction.isActive
            activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                onActiveStateChanged(transaction, isChecked)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(transaction)
            }
        }
    }

    private class AutomaticTransactionDiffCallback : DiffUtil.ItemCallback<AutomaticTransaction>() {
        override fun areItemsTheSame(oldItem: AutomaticTransaction, newItem: AutomaticTransaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AutomaticTransaction, newItem: AutomaticTransaction): Boolean {
            return oldItem == newItem
        }
    }
} 