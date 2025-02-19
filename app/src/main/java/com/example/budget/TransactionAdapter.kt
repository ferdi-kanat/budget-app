package com.example.budget

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private var transactions: List<TransactionEntity>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
        val textViewReceiptNumber: TextView = itemView.findViewById(R.id.textViewReceiptNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.textViewDate.text = transaction.date
        holder.textViewDescription.text = transaction.description
        holder.textViewAmount.text = "${transaction.amount} TL"
        holder.textViewReceiptNumber.text= "Fiş No: ${transaction.transactionId}" // Yeni bir alan ekleyin

    }

    override fun getItemCount(): Int {
        return transactions.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newTransactions: List<TransactionEntity>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }
}
