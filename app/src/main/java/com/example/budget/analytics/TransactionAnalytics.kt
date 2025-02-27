package com.example.budget.analytics

import com.example.budget.TransactionEntity
import org.apache.commons.math3.util.FastMath.abs

class TransactionAnalytics {
    private val categories = mapOf(
        "MARKET" to listOf("market", "grocery", "carrefour", "migros", "bim", "a101"),
        "YEMEK" to listOf("restaurant", "cafe", "yemek", "food"),
        "ULAŞIM" to listOf("akbil", "benzin", "taksi", "uber"),
        "FATURA" to listOf("elektrik", "su", "doğalgaz", "internet", "telefon"),
        "ALIŞVERİŞ" to listOf("giyim", "shopping", "hepsiburada", "trendyol")
    )

    fun analyzeTransactions(transactions: List<TransactionEntity>): AnalyticsResult {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { it.amount }
        val balance = totalIncome + totalExpense
        
        val monthlyBreakdown = calculateMonthlyTotals(transactions)
        val categoryBreakdown = calculateCategoryBreakdown(transactions)
        val bankBreakdown = calculateBankwiseTotal(transactions)

        return AnalyticsResult(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance,
            monthlyBreakdown = monthlyBreakdown,
            categoryBreakdown = categoryBreakdown,
            bankBreakdown = bankBreakdown
        )
    }

    private fun calculateCategoryBreakdown(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions
            .groupBy { determineCategory(it.description.lowercase()) }
            .mapValues { (_, transactions) -> 
                transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
            }
    }

    private fun determineCategory(description: String): String {
        categories.forEach { (category, keywords) ->
            if (keywords.any { description.contains(it) }) {
                return category
            }
        }
        return "DİĞER"
    }

    private fun calculateMonthlyTotals(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions
            .groupBy { it.date.substring(3, 10) } // "DD.MM.YYYY" -> "MM.YYYY"
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }
    }

    private fun calculateBankwiseTotal(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions
            .groupBy { it.bankName }
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }
    }
} 