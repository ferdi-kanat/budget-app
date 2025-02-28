package com.example.budget

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(private var transactions: List<TransactionEntity>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var onItemClickListener: ((TransactionEntity) -> Unit)? = null
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

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
        val context = holder.itemView.context

        // Temel bilgileri güvenli şekilde ayarla
        holder.description.text = transaction.description
        holder.date.text = transaction.date
        holder.bank.text = transaction.bankName

        // Para birimini formatla
        val amount = transaction.amount
        holder.amount.apply {
            text = numberFormat.format(amount)
            setTextColor(ContextCompat.getColor(context,
                if (amount >= 0) R.color.positive_amount else R.color.negative_amount))
        }

        // Kategori chip'ini ayarla - String'den TransactionCategory'ye dönüştür
        holder.categoryChip.apply {
            // String olarak saklanan kategori adını TransactionCategory'ye dönüştür
            val categoryEnum = TransactionCategory.fromDisplayName(transaction.category)

            text = categoryEnum.displayName

            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, categoryEnum.colorResId))

            setTextColor(Color.WHITE)

            contentDescription = try {
                context.getString(
                    R.string.category_chip_description_format,
                    categoryEnum.displayName
                )
            } catch (e: Exception) {
                categoryEnum.displayName
            }
        }

        // Click listener'ı ayarla
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