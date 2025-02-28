package com.example.budget

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budget.data.dao.BudgetGoalDao

object DatabaseProvider {
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "budget_database"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Geçici tablo oluştur
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions_new (
                    transactionId TEXT PRIMARY KEY NOT NULL,
                    date TEXT NOT NULL,
                    time TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amount REAL NOT NULL,
                    balance REAL,
                    bankName TEXT NOT NULL,
                    category TEXT NOT NULL DEFAULT 'OTHER'
                )
            """
            )

            // Verileri yeni tabloya kopyala
            db.execSQL(
                """
                INSERT OR REPLACE INTO transactions_new 
                SELECT transactionId, date, time, description, amount, balance, bankName, 'OTHER' 
                FROM transactions
            """
            )

            // Eski tabloyu sil
            db.execSQL("DROP TABLE transactions")

            // Yeni tabloyu yeniden adlandır
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

            // Index oluştur
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_id ON transactions(transactionId)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS budget_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category TEXT NOT NULL,
                    monthYear TEXT NOT NULL,
                    targetAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    UNIQUE(category, monthYear)
                )
            """)
        }
    }

    fun getTransactionDao(): TransactionDao {
        return database?.transactionDao() ?: throw IllegalStateException("Database not initialized")
    }

    fun getBudgetGoalDao(context: Context): BudgetGoalDao {
        if (database == null) {
            initialize(context)
        }
        return database?.budgetGoalDao() ?: throw IllegalStateException("Database not initialized")
    }
}