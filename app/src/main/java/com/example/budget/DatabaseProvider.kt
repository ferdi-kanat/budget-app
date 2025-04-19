package com.example.budget

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budget.data.dao.AccountDao
import com.example.budget.data.dao.AutomaticTransactionDao
import com.example.budget.data.dao.TransactionDao
import com.example.budget.data.dao.BudgetGoalDao

object DatabaseProvider {
    private var database: AppDatabase? = null
    private var transactionDao: TransactionDao? = null
    private var accountDao: AccountDao? = null
    private var automaticTransactionDao: AutomaticTransactionDao? = null
    private var budgetGoalDao: BudgetGoalDao? = null

    fun initialize(context: Context) {
        if (database == null) {
            getDatabase(context)
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "budget_database"
            )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            )
            .fallbackToDestructiveMigration()
            .build()
            database = instance
            instance
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add any necessary schema changes for version 1 to 2
            database.execSQL("ALTER TABLE automatic_transactions ADD COLUMN description TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add any necessary schema changes for version 2 to 3
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add any necessary schema changes for version 3 to 4
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create budget_goals table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    monthYear TEXT NOT NULL,
                    targetAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """)
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop and recreate budget_goals table
            database.execSQL("DROP TABLE IF EXISTS budget_goals")
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    monthYear TEXT NOT NULL,
                    targetAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """)
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create automatic_transactions table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS automatic_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    accountId INTEGER NOT NULL,
                    repeatPeriod TEXT NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                )
            """)
        }
    }

    fun getTransactionDao(): TransactionDao {
        return transactionDao ?: database?.transactionDao()?.also { transactionDao = it }
            ?: throw IllegalStateException("Database not initialized")
    }

    fun getAccountDao(context: Context): AccountDao {
        if (database == null) {
            getDatabase(context)
        }
        return database?.accountDao() ?: throw IllegalStateException("Database not initialized")
    }

    fun getAutomaticTransactionDao(): AutomaticTransactionDao {
        return automaticTransactionDao ?: database?.automaticTransactionDao()?.also { automaticTransactionDao = it }
            ?: throw IllegalStateException("Database not initialized")
    }

    fun getBudgetGoalDao(context: Context): BudgetGoalDao {
        if (database == null) {
            getDatabase(context)
        }
        return budgetGoalDao ?: database?.budgetGoalDao()?.also { budgetGoalDao = it }
            ?: throw IllegalStateException("Database not initialized")
    }
}