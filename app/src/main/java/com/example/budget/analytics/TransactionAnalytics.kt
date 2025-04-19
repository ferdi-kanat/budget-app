package com.example.budget.analytics

import com.example.budget.TransactionEntity
import java.util.Collections

class TransactionAnalytics {
    fun analyzeTransactions(transactions: List<TransactionEntity>): AnalyticsResult {
        val bankBreakdown = calculateBankwiseTotal(transactions)
        val balance = bankBreakdown.values.sum()
        
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = -transactions.filter { it.amount < 0 }.sumOf { it.amount }
        
        val monthlyBreakdown = calculateMonthlyTotals(transactions)
        val categoryBreakdown = calculateCategoryBreakdown(transactions)

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
            .groupBy { it.category } // Doğrudan String olarak kategoriyi kullan
            .mapValues { (_, transactions) ->
                -transactions.sumOf { it.amount } // Negate the sum of negative amounts
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
            .mapValues { (bankName, bankTransactions) -> 
                when (bankName) {
                    "Manuel Giriş" -> {
                        // For manual transactions, sum up all amounts
                        bankTransactions.sumOf { it.amount }
                    }
                    else -> {
                        // For bank transactions, use the final balance
                        val sortedTransactions = bankTransactions.sortedWith(
                            compareBy<TransactionEntity> { 
                                val parts = it.date.split(".")
                                "${parts[2]}${parts[1]}${parts[0]}"
                            }.thenBy { it.time }
                        )
                        sortedTransactions.lastOrNull()?.balance ?: 0.0
                    }
                }
            }
    }
} 