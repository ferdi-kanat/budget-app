package com.example.budget
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
}
