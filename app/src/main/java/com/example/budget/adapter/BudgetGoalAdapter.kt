package com.example.budget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import com.example.budget.data.BudgetProgress
import com.google.android.material.progressindicator.LinearProgressIndicator

class BudgetGoalAdapter(
    private val onEditClick: (BudgetProgress) -> Unit,
    private val onDeleteClick: (BudgetProgress) -> Unit
) : ListAdapter<BudgetProgress, BudgetGoalAdapter.ViewHolder>(BudgetProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val budgetProgress = getItem(position)
        holder.bind(budgetProgress)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryText)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressIndicator)
        private val spentTextView: TextView = itemView.findViewById(R.id.spentText)
        private val targetTextView: TextView = itemView.findViewById(R.id.targetText)

        fun bind(budgetProgress: BudgetProgress) {
            categoryTextView.text = budgetProgress.category
            val progress = (budgetProgress.spentAmount / budgetProgress.targetAmount * 100).toInt()
            progressBar.progress = progress
            spentTextView.text = itemView.context.getString(
                R.string.spent_amount_format,
                budgetProgress.spentAmount
            )
            targetTextView.text = itemView.context.getString(
                R.string.target_amount_format,
                budgetProgress.targetAmount
            )

            itemView.setOnClickListener { onEditClick(budgetProgress) }
            itemView.setOnLongClickListener { 
                onDeleteClick(budgetProgress)
                true
            }
        }
    }
}

private class BudgetProgressDiffCallback : DiffUtil.ItemCallback<BudgetProgress>() {
    override fun areItemsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress): Boolean {
        return oldItem.category == newItem.category
    }

    override fun areContentsTheSame(oldItem: BudgetProgress, newItem: BudgetProgress): Boolean {
        return oldItem == newItem
    }
}