package com.example.budget

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "budget_database"
            )
            .addMigrations(MIGRATION_3_4)
            .build()
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Geçici tablo oluştur
            db.execSQL("""
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
            """)

            // Verileri yeni tabloya kopyala
            db.execSQL("""
                INSERT OR REPLACE INTO transactions_new 
                SELECT transactionId, date, time, description, amount, balance, bankName, 'OTHER' 
                FROM transactions
            """)

            // Eski tabloyu sil
            db.execSQL("DROP TABLE transactions")

            // Yeni tabloyu yeniden adlandır
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

            // Index oluştur
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_id ON transactions(transactionId)")
        }
    }

    fun getTransactionDao(): TransactionDao {
        return database?.transactionDao() ?: throw IllegalStateException("Database not initialized")
    }
}

