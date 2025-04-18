package com.example.budget.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class AutoBackupManager(private val context: Context) {

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup_work"
    }

    fun scheduleWeeklyBackup() {
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
    }

    fun cancelAutoBackup() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
    }
} 