package com.example.budget

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budget.data.dao.BudgetGoalDao
import com.example.budget.data.entity.BudgetGoalEntity

@Database(entities = [TransactionEntity::class, BudgetGoalEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetGoalDao(): BudgetGoalDao
}