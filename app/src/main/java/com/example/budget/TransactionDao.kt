package com.example.budget

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        ORDER BY 
            SUBSTR(date, 7, 4) DESC,  -- Yıl (dd.MM.yyyy formatından yyyy)
            SUBSTR(date, 4, 2) DESC,  -- Ay (dd.MM.yyyy formatından MM)
            SUBSTR(date, 1, 2) DESC,  -- Gün (dd.MM.yyyy formatından dd)
            time DESC                 -- Aynı tarihli işlemler için saat sıralaması
    """)
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?
}
