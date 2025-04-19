package com.example.budget.data.dao

import androidx.room.*
import com.example.budget.data.AutomaticTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomaticTransactionDao {
    @Query("SELECT * FROM automatic_transactions ORDER BY payment_date DESC")
    fun getAllTransactions(): Flow<List<AutomaticTransaction>>

    @Query("SELECT * FROM automatic_transactions WHERE is_active = 1 ORDER BY payment_date DESC")
    fun getActiveTransactions(): Flow<List<AutomaticTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: AutomaticTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: AutomaticTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: AutomaticTransaction)

    @Query("UPDATE automatic_transactions SET last_processed_date = :date WHERE id = :transactionId")
    suspend fun updateLastProcessedDate(transactionId: Long, date: Long)

    @Query("SELECT * FROM automatic_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): AutomaticTransaction?
} 