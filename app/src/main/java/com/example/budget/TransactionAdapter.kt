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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionAdapter(private var transactions: List<TransactionEntity>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: ((TransactionEntity) -> Unit)? = null
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))
    private val groupedTransactions = mutableMapOf<String, List<TransactionEntity>>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    init {
        setHasStableIds(true)
        groupTransactions()
    }

    private fun groupTransactions() {
        groupedTransactions.clear()
        
        // Parse dates and sort transactions in descending order
        val sortedTransactions = transactions.sortedWith(compareByDescending { transaction ->
            try {
                val parts = transaction.date.split(".")
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                year * 10000 + month * 100 + day // Create a sortable integer
            } catch (e: Exception) {
                0 // If date parsing fails, put it at the end
            }
        })
        
        // Group by date and sort transactions within each group by time
        sortedTransactions.groupBy { it.date }.forEach { (date, transactions) ->
            groupedTransactions[date] = transactions.sortedWith(compareByDescending { it.time })
        }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0
        groupedTransactions.forEach { (_, transactions) ->
            if (position == currentPosition) return TYPE_HEADER
            currentPosition++
            if (position < currentPosition + transactions.size) return TYPE_TRANSACTION
            currentPosition += transactions.size
        }
        return TYPE_TRANSACTION
    }

    override fun getItemId(position: Int): Long {
        var currentPosition = 0
        groupedTransactions.forEach { (date, transactions) ->
            if (position == currentPosition) return date.hashCode().toLong()
            currentPosition++
            if (position < currentPosition + transactions.size) {
                return transactions[position - currentPosition].transactionId.hashCode().toLong()
            }
            currentPosition += transactions.size
        }
        return position.toLong()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayHeader: TextView = view.findViewById(R.id.textViewDayHeader)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.textViewDescription)
        val date: TextView = view.findViewById(R.id.textViewDate)
        val amount: TextView = view.findViewById(R.id.textViewAmount)
        val bank: TextView = view.findViewById(R.id.textViewBank)
        val categoryChip: Chip = view.findViewById(R.id.categoryChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.day_header_item, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.transaction_item, parent, false)
                TransactionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = 0
        var found = false

        for ((date, transactions) in groupedTransactions) {
            if (position == currentPosition) {
                if (holder is HeaderViewHolder) {
                    holder.dayHeader.text = formatDateHeader(date)
                    found = true
                    break
                }
            }
            currentPosition++

            if (position < currentPosition + transactions.size) {
                if (holder is TransactionViewHolder) {
                    val transaction = transactions[position - currentPosition]
                    bindTransactionViewHolder(holder, transaction)
                    found = true
                    break
                }
            }
            currentPosition += transactions.size
        }

        if (!found) {
            // Handle any remaining positions
            if (holder is TransactionViewHolder) {
                val lastGroup = groupedTransactions.values.lastOrNull()
                if (!lastGroup.isNullOrEmpty()) {
                    val lastTransaction = lastGroup.last()
                    bindTransactionViewHolder(holder, lastTransaction)
                }
            }
        }
    }

    private fun formatDateHeader(date: String): String {
        return try {
            val parsedDate = dateFormat.parse(date)
            val calendar = Calendar.getInstance().apply {
                if (parsedDate != null) {
                    time = parsedDate
                }
            }
            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Pazartesi"
                Calendar.TUESDAY -> "Salı"
                Calendar.WEDNESDAY -> "Çarşamba"
                Calendar.THURSDAY -> "Perşembe"
                Calendar.FRIDAY -> "Cuma"
                Calendar.SATURDAY -> "Cumartesi"
                Calendar.SUNDAY -> "Pazar"
                else -> ""
            }
            "$date - $dayOfWeek"
        } catch (e: Exception) {
            date
        }
    }

    private fun bindTransactionViewHolder(holder: TransactionViewHolder, transaction: TransactionEntity) {
        val context = holder.itemView.context

        holder.description.text = transaction.description
        holder.date.text = transaction.date
        holder.bank.text = transaction.bankName

        val amount = transaction.amount
        holder.amount.apply {
            text = numberFormat.format(amount)
            setTextColor(ContextCompat.getColor(context,
                if (amount >= 0) R.color.positive_amount else R.color.negative_amount))
        }

        holder.categoryChip.apply {
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

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(transaction)
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        groupedTransactions.forEach { (_, transactions) ->
            count += 1 + transactions.size // 1 for header + transactions
        }
        return count
    }

    fun submitNewData(newTransactions: List<TransactionEntity>) {
        val oldTransactions = transactions
        transactions = newTransactions.toList()
        
        // Clear the old grouped transactions
        groupedTransactions.clear()
        
        // Group the new transactions
        groupTransactions()
        
        // Notify the adapter of the complete data set change
        notifyDataSetChanged()
    }

    fun updateData(newTransactions: List<TransactionEntity>) {
        val oldTransactions = transactions
        transactions = newTransactions.toList()
        
        // Clear the old grouped transactions
        groupedTransactions.clear()
        
        // Group the new transactions
        groupTransactions()
        
        // Notify the adapter of the complete data set change
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (TransactionEntity) -> Unit) {
        onItemClickListener = listener
    }
}