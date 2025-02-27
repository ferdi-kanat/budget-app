package com.example.budget.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class CategoryAdapter(categories: Map<String, Double>) : 
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    private val sortedCategories = categories.entries
        .filter { it.value < 0 }
        .sortedByDescending { abs(it.value) }
        .toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.textViewCategoryName)
        val amount: TextView = view.findViewById(R.id.textViewAmount)
        val percentage: TextView = view.findViewById(R.id.textViewPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = sortedCategories[position]
        val totalExpense = sortedCategories.sumOf { abs(it.value) }
        val percentage = abs(category.value) / totalExpense * 100

        holder.categoryName.text = category.key
        holder.amount.text = formatter.format(abs(category.value))
        holder.percentage.text = String.format("%.1f%%", percentage)
    }

    override fun getItemCount() = sortedCategories.size
} 