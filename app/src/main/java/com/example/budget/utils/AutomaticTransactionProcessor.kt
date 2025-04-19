package com.example.budget.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budget.DatabaseProvider
import com.example.budget.data.AutomaticTransaction
import com.example.budget.data.RepeatPeriod
import com.example.budget.TransactionEntity
import com.example.budget.TransactionCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.*

class AutomaticTransactionProcessor(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            processAutomaticTransactions()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun processAutomaticTransactions() {
        val automaticTransactionDao = DatabaseProvider.getAutomaticTransactionDao()
        val transactionDao = DatabaseProvider.getTransactionDao()
        val accountDao = DatabaseProvider.getAccountDao(applicationContext)
        
        // Get all active automatic transactions
        val activeTransactions = automaticTransactionDao.getActiveTransactions().first()
        
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        for (transaction in activeTransactions) {
            // Skip if the payment date is in the future
            if (transaction.paymentDate > currentTime) {
                continue
            }
            
            // For ONE_TIME transactions, process only if not processed before
            if (transaction.repeatPeriod == RepeatPeriod.ONE_TIME) {
                if (transaction.lastProcessedDate != null) {
                    // Already processed, skip
                    continue
                }
                
                // Process the transaction
                processTransaction(transaction, transactionDao, accountDao)
                
                // Mark as processed
                automaticTransactionDao.updateLastProcessedDate(transaction.id, currentTime)
                
                // Deactivate the transaction after processing
                automaticTransactionDao.updateTransaction(transaction.copy(isActive = false))
                
                continue
            }
            
            // For recurring transactions, check if it's time to process
            val lastProcessedDate = transaction.lastProcessedDate ?: 0L
            
            // Calculate the next processing date based on repeat period
            val nextProcessingDate = calculateNextProcessingDate(transaction.paymentDate, transaction.repeatPeriod, lastProcessedDate)
            
            // If it's time to process
            if (currentTime >= nextProcessingDate) {
                // Process the transaction
                processTransaction(transaction, transactionDao, accountDao)
                
                // Update last processed date
                automaticTransactionDao.updateLastProcessedDate(transaction.id, currentTime)
            }
        }
    }
    
    private fun calculateNextProcessingDate(initialDate: Long, repeatPeriod: RepeatPeriod, lastProcessedDate: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = if (lastProcessedDate > 0) lastProcessedDate else initialDate
        
        when (repeatPeriod) {
            RepeatPeriod.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            RepeatPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RepeatPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RepeatPeriod.ONE_TIME -> return initialDate // Should not reach here
        }
        
        return calendar.timeInMillis
    }
    
    private suspend fun processTransaction(
        automaticTransaction: AutomaticTransaction,
        transactionDao: com.example.budget.data.dao.TransactionDao,
        accountDao: com.example.budget.data.dao.AccountDao
    ) {
        // Get the account
        val accounts = accountDao.getAllAccounts().first()
        val account = accounts.find { it.accountId == automaticTransaction.accountId.toString() }
            ?: return
        
        // Create a new transaction
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = automaticTransaction.paymentDate
        
        val date = String.format(
            "%02d.%02d.%04d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
        
        val time = String.format(
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
        
        val transaction = TransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            date = date,
            time = time,
            description = automaticTransaction.description,
            amount = automaticTransaction.amount,
            balance = 0.0, // Will be updated by the account
            bankName = account.bankName,
            category = TransactionCategory.fromDescription(automaticTransaction.description).displayName
        )
        
        // Insert the transaction
        transactionDao.insertTransaction(transaction)
        
        // Update account balance
        val newBalance = account.balance - automaticTransaction.amount
        accountDao.updateBalance(account.accountId, newBalance)
    }
} 