package com.example.budget

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class TransactionAdapter(private var transactions: List<TransactionEntity>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var onItemClickListener: ((TransactionEntity) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return transactions[position].transactionId.hashCode().toLong()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.textViewDescription)
        val date: TextView = view.findViewById(R.id.textViewDate)
        val amount: TextView = view.findViewById(R.id.textViewAmount)
        val bank: TextView = view.findViewById(R.id.textViewBank)
        val categoryChip: Chip = view.findViewById(R.id.categoryChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        
        holder.description.text = transaction.description
        holder.date.text = transaction.date
        holder.bank.text = transaction.bankName
        
        // Tutarı formatlama
        val amount = transaction.amount
        holder.amount.text = String.format("%.2f ₺", amount)
        holder.amount.setTextColor(if (amount >= 0) 
            Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

        // Kategori chip'ini ayarlama
        holder.categoryChip.apply {
            text = transaction.category.displayName
            chipBackgroundColor = ColorStateList.valueOf(transaction.category.color)
            setTextColor(Color.WHITE)
            contentDescription = holder.itemView.context.getString(
                R.string.category_chip_description_format,
                transaction.category.displayName
            )
        }

        // Add click listener to the entire item
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<TransactionEntity>) {
        val diffCallback = TransactionDiffCallback(transactions, newTransactions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        transactions = newTransactions
        diffResult.dispatchUpdatesTo(this)
    }

    fun setOnItemClickListener(listener: (TransactionEntity) -> Unit) {
        onItemClickListener = listener
    }

    private class TransactionDiffCallback(
        private val oldList: List<TransactionEntity>,
        private val newList: List<TransactionEntity>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].transactionId == newList[newItemPosition].transactionId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
