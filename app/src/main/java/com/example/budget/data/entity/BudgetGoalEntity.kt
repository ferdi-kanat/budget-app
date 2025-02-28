package com.example.budget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // TransactionCategory.displayName ile aynı değeri kullanacak
    val monthYear: String, // Format: "YYYY-MM"
    val targetAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)