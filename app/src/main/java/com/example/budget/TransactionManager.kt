package com.example.budget

import androidx.lifecycle.lifecycleScope
import com.example.budget.database.DatabaseProvider
import com.example.budget.database.TransactionEntity
import com.example.budget.models.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TransactionManager(private val activity: MainActivity) {

    fun saveTransaction(description: String, amount: Double) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                date = getCurrentDate(),
                time = getCurrentTime(),
                transactionId = UUID.randomUUID().toString(),
                amount = amount.toString(),
                balance = "0.0",
                description = description
            )
            val transactionEntity = TransactionEntity(
                date = transaction.date,
                time = transaction.time,
                transactionId = transaction.transactionId,
                amount = transaction.amount.toDoubleOrNull() ?: 0.0,
                balance = transaction.balance.toDoubleOrNull(),
                description = transaction.description,
                bankName = "" // Add appropriate bank name if needed
            )
            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)
            withContext(Dispatchers.Main) {
                activity.refreshRecyclerView()
            }
        }
    }

    suspend fun saveTransactionsToDatabase(transactions: List<Transaction>, bankName: String) {
        transactions.forEach { transaction ->
            val transactionEntity = TransactionEntity(
                date = transaction.date,
                time = if (bankName == "Ziraat Bankası") null else transaction.time,
                transactionId = transaction.transactionId,
                amount = transaction.amount.toDoubleOrNull() ?: 0.0,
                balance = transaction.balance.toDoubleOrNull(),
                description = transaction.description,
                bankName = bankName
            )
            DatabaseProvider.getTransactionDao().insertTransaction(transactionEntity)
        }
        withContext(Dispatchers.Main) {
            activity.refreshRecyclerView()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
