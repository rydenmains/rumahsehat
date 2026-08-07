package com.rumahsehat.data.repository

import com.rumahsehat.data.dao.AssessmentDao
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.data.remote.AssessmentSync
import kotlinx.coroutines.flow.Flow

class AssessmentRepository(private val assessmentDao: AssessmentDao) {
    val allAssessments: Flow<List<Assessment>> = assessmentDao.getAllAssessments()

    suspend fun insert(assessment: Assessment, items: List<ScoreItem>) {
        assessmentDao.insertAssessment(assessment)
        assessmentDao.insertScoreItems(items)
    }

    suspend fun getAssessmentById(id: String): Assessment? {
        return assessmentDao.getAssessmentById(id)
    }

    suspend fun getScoreItemsForAssessment(assessmentId: String): List<ScoreItem> {
        return assessmentDao.getScoreItemsForAssessment(assessmentId)
    }

    suspend fun syncPending() {
        for (a in assessmentDao.getPendingAssessments()) {
            AssessmentSync.push(assessmentDao, a, assessmentDao.getScoreItemsForAssessment(a.id))
        }
    }
}
