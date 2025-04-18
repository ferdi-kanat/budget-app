package com.example.budget.data.dao

import androidx.room.*
import com.example.budget.data.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE accountId = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE accountId = :accountId")
    suspend fun updateBalance(accountId: String, amount: Double)

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1")
    suspend fun getTotalBalance(): Double?

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
} 