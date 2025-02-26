package com.example.budget.analytics

import java.io.Serializable

data class AnalyticsResult(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val monthlyTotals: Map<String, Double> = emptyMap(),
    val bankwiseTotal: Map<String, Double> = emptyMap()
) : Serializable 