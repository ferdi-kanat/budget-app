package com.example.budget.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budget.DatabaseProvider
import com.example.budget.data.RepeatPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class AutomaticTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = DatabaseProvider.getAutomaticTransactionDao()
            val activeTransactions = dao.getActiveTransactions().first()
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            activeTransactions.forEach { transaction ->
                val transactionDate = Calendar.getInstance().apply {
                    timeInMillis = transaction.paymentDate
                }

                // Check if it's time to process this transaction
                if (shouldProcessTransaction(transactionDate, calendar, transaction.repeatPeriod)) {
                    // Create the transaction
                    val transactionDao = DatabaseProvider.getTransactionDao()
                    transactionDao.insertTransaction(transaction.toTransaction())

                    // Update the next payment date
                    val nextPaymentDate = calculateNextPaymentDate(transactionDate, transaction.repeatPeriod)
                    dao.updateTransaction(transaction.copy(
                        paymentDate = nextPaymentDate.timeInMillis,
                        lastProcessedDate = currentTime
                    ))
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun shouldProcessTransaction(
        transactionDate: Calendar,
        currentDate: Calendar,
        repeatPeriod: RepeatPeriod
    ): Boolean {
        return when (repeatPeriod) {
            RepeatPeriod.DAILY -> true
            RepeatPeriod.WEEKLY -> {
                val dayOfWeek = transactionDate.get(Calendar.DAY_OF_WEEK)
                currentDate.get(Calendar.DAY_OF_WEEK) == dayOfWeek
            }
            RepeatPeriod.MONTHLY -> {
                val dayOfMonth = transactionDate.get(Calendar.DAY_OF_MONTH)
                currentDate.get(Calendar.DAY_OF_MONTH) == dayOfMonth
            }
            RepeatPeriod.YEARLY -> {
                val dayOfYear = transactionDate.get(Calendar.DAY_OF_YEAR)
                currentDate.get(Calendar.DAY_OF_YEAR) == dayOfYear
            }
            RepeatPeriod.ONE_TIME -> {
                val dayOfYear = transactionDate.get(Calendar.DAY_OF_YEAR)
                val year = transactionDate.get(Calendar.YEAR)
                currentDate.get(Calendar.DAY_OF_YEAR) == dayOfYear && 
                currentDate.get(Calendar.YEAR) == year
            }
        }
    }

    private fun calculateNextPaymentDate(
        currentDate: Calendar,
        repeatPeriod: RepeatPeriod
    ): Calendar {
        val nextDate = currentDate.clone() as Calendar
        when (repeatPeriod) {
            RepeatPeriod.DAILY -> nextDate.add(Calendar.DAY_OF_MONTH, 1)
            RepeatPeriod.WEEKLY -> nextDate.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatPeriod.MONTHLY -> nextDate.add(Calendar.MONTH, 1)
            RepeatPeriod.YEARLY -> nextDate.add(Calendar.YEAR, 1)
            RepeatPeriod.ONE_TIME -> { /* No need to calculate next date for one-time transactions */ }
        }
        return nextDate
    }
} 