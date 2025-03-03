package com.example.budget.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.budget.R
import java.text.NumberFormat
import java.util.Locale
import android.os.Build
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.components.XAxis
import kotlin.math.abs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var analytics: AnalyticsResult
    private lateinit var monthlyChart: LineChart
    private lateinit var categoryChart: PieChart
    private lateinit var bankChart: BarChart
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        showLoading()
        
        initializeViews()
        setupRecyclerView()
        
        analytics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("analytics", AnalyticsResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("analytics")
        } ?: return

        setupCharts()
    }

    private fun initializeViews() {
        monthlyChart = findViewById(R.id.monthlyChart)
        categoryChart = findViewById(R.id.categoryChart)
        bankChart = findViewById(R.id.bankChart)

        // Pre-configure charts
        monthlyChart.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
        }

        categoryChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            centerText = "Harcama\nDağılımı"
            setCenterTextSize(14f)
            setDrawEntryLabels(false)
            legend.isEnabled = true
        }

        bankChart.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
    }

    private fun setupCharts() {
        lifecycleScope.launch(Dispatchers.Default) {
            val monthlyData = prepareMonthlyData()
            val categoryData = prepareCategoryData()
            val bankData = prepareBankData()

            withContext(Dispatchers.Main) {
                displayMonthlyChart(monthlyData)
                displayCategoryChart(categoryData)
                displayBankChart(bankData)
                setupGeneralOverview()
                setupCategoryList()
                hideLoading()
            }
        }
    }

    private fun displayMonthlyChart(data: LineData) {
        monthlyChart.apply {
            this.data = data
            animateY(300)
            invalidate()
        }
    }

    private fun displayCategoryChart(data: PieData) {
        categoryChart.apply {
            this.data = data
            animateY(300)
            invalidate()
        }
    }

    private fun displayBankChart(data: BarData) {
        bankChart.apply {
            this.data = data
            animateY(300)
            invalidate()
        }
    }

    private fun prepareMonthlyData(): LineData {
        val entries = analytics.monthlyBreakdown.entries
            .sortedBy { it.key }
            .map { Entry(it.key.substring(5).toFloat(), it.value.toFloat()) }

        return LineData(LineDataSet(entries, "Aylık Harcama").apply {
            color = getColor(R.color.primary)
            valueTextSize = 12f
            setDrawFilled(true)
            fillColor = getColor(R.color.primary_light)
        })
    }

    private fun setupGeneralOverview() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        
        findViewById<TextView>(R.id.textViewTotalIncome).text = 
            getString(R.string.total_income_format, formatter.format(analytics.totalIncome))
        findViewById<TextView>(R.id.textViewTotalExpense).text = 
            getString(R.string.total_expense_format, formatter.format(analytics.totalExpense))
        findViewById<TextView>(R.id.textViewBalance).text = 
            getString(R.string.balance_format, formatter.format(analytics.balance))
    }

    private fun prepareCategoryData(): PieData {
        val entries = analytics.categoryBreakdown.entries
            .filter { it.value < 0 } // Sadece harcamaları göster
            .map { PieEntry(abs(it.value.toFloat()), it.key) }

        return PieData(PieDataSet(entries, "Kategori Dağılımı").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueFormatter = PercentFormatter(findViewById(R.id.categoryChart))
        })
    }

    private fun prepareBankData(): BarData {
        val entries = analytics.bankBreakdown.entries
            .mapIndexed { index, entry -> 
                BarEntry(index.toFloat(), abs(entry.value.toFloat()))
            }

        return BarData(BarDataSet(entries, "Banka Dağılımı").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        })
    }

    private fun setupCategoryList() {
        recyclerView.adapter = CategoryAdapter(analytics.categoryBreakdown)
    }

    private fun showLoading() {
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
    }

    private fun hideLoading() {
        findViewById<View>(R.id.progressBar).visibility = View.GONE
    }
} 