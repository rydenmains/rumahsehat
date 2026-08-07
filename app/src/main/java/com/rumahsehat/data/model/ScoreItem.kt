package com.rumahsehat.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "score_items",
    foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assessmentId")]
)
data class ScoreItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assessmentId: String,
    val itemId: String, // 1.1, 2.1, etc.
    val score: Int,
    val isApplicable: Boolean = true,
    val reason: String? = null
)
