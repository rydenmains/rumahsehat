package com.rumahsehat.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rumahsehat.data.db.AppDatabase
import com.rumahsehat.data.repository.AssessmentRepository

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).assessmentDao()
        val repository = AssessmentRepository(dao)
        return try {
            repository.syncPending()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}