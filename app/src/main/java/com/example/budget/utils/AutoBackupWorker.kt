package com.example.budget.utils

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budget.DatabaseProvider
import com.google.gson.Gson
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val BACKUP_FILENAME_PREFIX = "auto_backup_"
        private const val BACKUP_EXTENSION = ".json"
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    override suspend fun doWork(): Result {
        return try {
            // Get all transactions
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions()
            
            // Create backup file in app's external files directory
            val filename = BACKUP_FILENAME_PREFIX + dateFormat.format(Date()) + BACKUP_EXTENSION
            val file = applicationContext.getExternalFilesDir(null)?.let { 
                File(it, filename)
            } ?: return Result.failure()

            // Write transactions to file
            file.outputStream().use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val gson = Gson()
                    val json = gson.toJson(transactions)
                    writer.write(json)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
} 