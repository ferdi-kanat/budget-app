package com.example.budget

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "budget_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    fun getTransactionDao(): TransactionDao {
        return database!!.transactionDao()
    }
}

