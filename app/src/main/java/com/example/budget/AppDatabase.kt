package com.example.budget

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budget.data.AccountEntity
import com.example.budget.data.AutomaticTransaction
import com.example.budget.data.dao.AccountDao
import com.example.budget.data.dao.AutomaticTransactionDao
import com.example.budget.data.dao.BudgetGoalDao
import com.example.budget.data.dao.TransactionDao
import com.example.budget.data.entity.BudgetGoalEntity
import com.example.budget.utils.Converters

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        AutomaticTransaction::class,
        BudgetGoalEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun automaticTransactionDao(): AutomaticTransactionDao
    abstract fun budgetGoalDao(): BudgetGoalDao

    companion object
}