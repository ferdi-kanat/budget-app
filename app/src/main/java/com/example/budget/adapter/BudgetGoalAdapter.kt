package com.example.budget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import com.example.budget.data.dao.BudgetProgress
import com.example.budget.databinding.ItemBudgetGoalBinding
import java.text.NumberFormat
import java.util.*

class BudgetGoalAdapter : ListAdapter<BudgetProgress, BudgetGoalAdapter.ViewHolder>(BudgetDiffCallback()) {
    private var onItemClickListener: ((BudgetProgress) -> Unit)? = null
    private var onItemLongClickListener: ((BudgetProgress) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBudgetGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClickListener, onItemLongClickListener)
    }
    fun setOnItemClickListener(listener: (BudgetProgress) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (BudgetProgress) -> Unit) {
        onItemLongClickListener = listener
    }

    class ViewHolder(private val binding: ItemBudgetGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        // ViewHolder sınıfı içindeki bind metodu
        fun bind(item: BudgetProgress, onClickListener: ((BudgetProgress) -> Unit)?, onLongClickListener: ((BudgetProgress) -> Unit)?) {
            binding.apply {
                categoryText.text = item.category

                // Format currency
                val currencyFormat = NumberFormat.getCurrencyInstance().apply {
                    currency = Currency.getInstance("TRY")
                }

                // Sorunlu root referansını itemView ile değiştirelim
                spentText.text = itemView.context.getString(
                    R.string.spent_amount_format,
                    currencyFormat.format(item.spentAmount)
                )

                targetText.text = itemView.context.getString(
                    R.string.target_amount_format,
                    currencyFormat.format(item.targetAmount)
                )

                // Calculate progress safely
                val progress = if (item.targetAmount > 0) {
                    ((item.spentAmount / item.targetAmount) * 100)
                        .toInt()
                        .coerceIn(0, 100)
                } else 0

                progressIndicator.progress = progress

                // Set color based on progress
                val color = when {
                    progress >= 90 -> R.color.error
                    progress >= 75 -> R.color.warning
                    else -> R.color.success
                }
                progressIndicator.setIndicatorColor(
                    ContextCompat.getColor(itemView.context, color)
                )
            }

            // Tıklama işleyicileri için binding.root yerine itemView kullanın
            itemView.setOnClickListener {
                onClickListener?.invoke(item)
            }

            itemView.setOnLongClickListener {
                onLongClickListener?.invoke(item)
                true
            }
        }
    }

    private class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetProgress>() {
        override fun areItemsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress): Boolean {
            return oldItem == newItem
        }
    }
}