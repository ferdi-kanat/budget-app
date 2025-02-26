package com.example.budget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView

class EditableTransactionAdapter(private val transactions: MutableList<MainActivity.Transaction>) :
    RecyclerView.Adapter<EditableTransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val editTextDescription: EditText = itemView.findViewById(R.id.editTextDescription)
        val editTextAmount: EditText = itemView.findViewById(R.id.editTextAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editable_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.textViewDate.text = transaction.date
        holder.editTextDescription.setText(transaction.description)
        holder.editTextAmount.setText(transaction.amount)

        holder.editTextDescription.addTextChangedListener {
            transactions[holder.adapterPosition] = transactions[holder.adapterPosition].copy(
                description = it.toString()
            )
        }

        holder.editTextAmount.addTextChangedListener {
            transactions[holder.adapterPosition] = transactions[holder.adapterPosition].copy(
                amount = it.toString()
            )
        }
    }

    override fun getItemCount(): Int = transactions.size
}
