package com.rumahsehat.data.dao

import androidx.room.*
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.data.model.ScoreItem
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssessment(assessment: Assessment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScoreItems(items: List<ScoreItem>): List<Long>

    @Query("SELECT * FROM assessments ORDER BY createdAt DESC")
    abstract fun getAllAssessments(): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments WHERE id = :id")
    abstract suspend fun getAssessmentById(id: String): Assessment?

    @Query("SELECT * FROM score_items WHERE assessmentId = :assessmentId")
    abstract suspend fun getScoreItemsForAssessment(assessmentId: String): List<ScoreItem>

    @Query("SELECT * FROM assessments WHERE syncStatus != 'SYNCED'")
    abstract suspend fun getPendingAssessments(): List<Assessment>

    @Query("UPDATE assessments SET syncStatus = :status WHERE id = :assessmentId")
    abstract suspend fun updateSyncStatus(assessmentId: String, status: String): Int

    @Transaction
    open suspend fun deleteAssessment(assessmentId: String): Int {
        val count = deleteScoreItemsForAssessment(assessmentId)
        deleteAssessmentEntry(assessmentId)
        return count
    }

    @Query("DELETE FROM score_items WHERE assessmentId = :assessmentId")
    abstract suspend fun deleteScoreItemsForAssessment(assessmentId: String): Int

    @Query("DELETE FROM assessments WHERE id = :assessmentId")
    abstract suspend fun deleteAssessmentEntry(assessmentId: String): Int
}
