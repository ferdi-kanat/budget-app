package com.example.budget.utils

import android.content.Context
import android.net.Uri
import com.example.budget.TransactionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class BackupUtils {
    companion object {
        private const val BACKUP_FILENAME_PREFIX = "budget_backup_"
        private const val BACKUP_EXTENSION = ".json"
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("tr", "TR"))

        fun createBackup(context: Context, transactions: List<TransactionEntity>, uri: Uri) {
            try {
                val gson = Gson()
                val json = gson.toJson(transactions)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
            } catch (e: Exception) {
                throw Exception("Backup creation failed: ${e.message}")
            }
        }

        fun restoreFromBackup(context: Context, uri: Uri): List<TransactionEntity> {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<TransactionEntity>>() {}.type
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val json = reader.readText()
                        return gson.fromJson(json, type)
                    }
                }
                throw Exception("Failed to read backup file")
            } catch (e: Exception) {
                throw Exception("Restore failed: ${e.message}")
            }
        }

        fun getBackupFilename(): String {
            return BACKUP_FILENAME_PREFIX + dateFormat.format(Date()) + BACKUP_EXTENSION
        }
    }
} 