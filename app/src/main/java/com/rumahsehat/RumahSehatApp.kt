package com.rumahsehat

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rumahsehat.sync.SyncWorker
import java.util.concurrent.TimeUnit

class RumahSehatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Retry kirim penilaian yang masih PENDING tiap aplikasi dibuka.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "sync-pending",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
        )
    }
}