package com.example.budget.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import java.text.NumberFormat
import java.util.Locale

class CategoryAdapter(private val categories: Map<String, Double>) : 
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    private val sortedCategories = categories.entries.sortedByDescending { it.value }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.textViewCategoryName)
        val categoryAmount: TextView = view.findViewById(R.id.textViewCategoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = sortedCategories[position]
        holder.categoryName.text = category.key
        holder.categoryAmount.text = formatter.format(category.value)
    }

    override fun getItemCount() = categories.size
} 