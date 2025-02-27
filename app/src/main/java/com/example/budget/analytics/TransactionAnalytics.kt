package com.example.budget.analytics

import com.example.budget.TransactionEntity
import org.apache.commons.math3.util.FastMath.abs
import java.util.Collections

class TransactionAnalytics {
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
        val categoryMap = transactions
            .filter { it.amount < 0 } // Önce sadece harcamaları filtrele
            .groupBy { it.category.displayName } // TransactionCategory'nin displayName'ini kullan
            .mapValues { (_, transactions) -> 
                transactions.sumOf { abs(it.amount) }
            }
        
        return categoryMap.filterValues { it > 0 } // Sıfırdan büyük değerleri filtrele
    }

    private fun calculateMonthlyTotals(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions
            .groupBy { 
                val parts = it.date.split(".")
                "${parts[2]}.${parts[1]}"
            }
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }
            .toSortedMap(Collections.reverseOrder())
    }

    private fun calculateBankwiseTotal(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions
            .groupBy { it.bankName }
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }
    }
} 