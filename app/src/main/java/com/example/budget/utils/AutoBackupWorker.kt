package com.example.budget.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budget.DatabaseProvider
import com.google.gson.Gson
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val BACKUP_FILENAME_PREFIX = "auto_backup_"
        private const val BACKUP_EXTENSION = ".json"
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("tr", "TR"))
        private const val TAG = "AutoBackupWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting automatic backup...")
            
            // Get all transactions
            val transactions = DatabaseProvider.getTransactionDao().getAllTransactions().first()
            
            if (transactions.isEmpty()) {
                Log.w(TAG, "No transactions found to backup")
                return Result.success()
            }
            
            // Create backup file in app's external files directory
            val filename = BACKUP_FILENAME_PREFIX + dateFormat.format(Date()) + BACKUP_EXTENSION
            val file = applicationContext.getExternalFilesDir(null)?.let { 
                File(it, filename)
            } ?: run {
                Log.e(TAG, "Failed to get external files directory")
                return Result.failure()
            }

            // Write transactions to file
            file.outputStream().use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val gson = Gson()
                    val json = gson.toJson(transactions)
                    writer.write(json)
                }
            }

            Log.d(TAG, "Backup completed successfully: ${file.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure()
        }
    }
} 