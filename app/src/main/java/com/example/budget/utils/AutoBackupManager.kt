package com.example.budget.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import java.util.concurrent.TimeUnit

class AutoBackupManager(private val context: Context) {

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup_work"
        private const val INITIAL_BACKUP_WORK_NAME = "initial_backup_work"
        private const val PREFS_NAME = "AutoBackupPrefs"
        private const val KEY_IS_ENABLED = "is_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutoBackupEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_ENABLED, false)
    }

    fun scheduleWeeklyBackup() {
        // Create initial backup immediately using OneTimeWorkRequest
        val initialBackupRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                INITIAL_BACKUP_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                initialBackupRequest
            )

        // Schedule periodic backups
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
            )
        
        prefs.edit().putBoolean(KEY_IS_ENABLED, true).apply()
    }

    fun cancelAutoBackup() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
        
        prefs.edit().putBoolean(KEY_IS_ENABLED, false).apply()
    }
} 