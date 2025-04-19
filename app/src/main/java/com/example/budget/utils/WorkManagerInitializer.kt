package com.example.budget.utils

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budget.workers.AutomaticTransactionWorker
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val workManager = WorkManager.getInstance(context)
        
        // Create constraints for the worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Create a periodic work request that runs every 15 minutes
        val automaticTransactionWorkRequest = PeriodicWorkRequestBuilder<AutomaticTransactionWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work request
        workManager.enqueueUniquePeriodicWork(
            "automatic_transaction_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            automaticTransactionWorkRequest
        )

        return workManager
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
} 