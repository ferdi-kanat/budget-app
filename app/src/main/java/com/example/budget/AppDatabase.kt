package com.example.budget

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budget.data.AccountEntity
import com.example.budget.data.dao.AccountDao
import com.example.budget.data.dao.BudgetGoalDao
import com.example.budget.data.entity.BudgetGoalEntity

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, BudgetGoalEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetGoalDao(): BudgetGoalDao
}