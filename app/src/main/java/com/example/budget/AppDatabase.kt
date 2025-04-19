package com.example.budget

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budget.data.AccountEntity
import com.example.budget.data.AutomaticTransaction
import com.example.budget.TransactionEntity
import com.example.budget.data.dao.AccountDao
import com.example.budget.data.dao.AutomaticTransactionDao
import com.example.budget.data.dao.TransactionDao
import com.example.budget.data.dao.BudgetGoalDao
import com.example.budget.utils.Converters
import com.example.budget.data.entity.BudgetGoalEntity

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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}