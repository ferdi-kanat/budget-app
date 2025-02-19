package com.example.budget

import android.content.Context

object DatabaseProvider {
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }

    fun getTransactionDao(): TransactionDao {
        return database!!.transactionDao()
    }
}

