package com.example.budget.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget.R
import java.text.NumberFormat
import java.util.Locale
import android.os.Build
import android.view.View
import android.widget.ProgressBar

class AnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        showLoading()

        val analytics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("analytics", AnalyticsResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("analytics") as? AnalyticsResult
        } ?: return

        setupGeneralOverview(analytics)
        setupCategoryBreakdown(analytics)

        hideLoading()
    }

    private fun setupGeneralOverview(analytics: AnalyticsResult) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        
        findViewById<TextView>(R.id.textViewTotalIncome).text = 
            getString(R.string.total_income_format, formatter.format(analytics.totalIncome))
        findViewById<TextView>(R.id.textViewTotalExpense).text = 
            getString(R.string.total_expense_format, formatter.format(analytics.totalExpense))
        findViewById<TextView>(R.id.textViewBalance).text = 
            getString(R.string.balance_format, formatter.format(analytics.balance))
    }

    private fun setupCategoryBreakdown(analytics: AnalyticsResult) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CategoryAdapter(analytics.categoryBreakdown)
    }

    private fun showLoading() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
    }

    private fun hideLoading() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
    }
} 