package com.rumahsehat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey
    val id: String, // ASM-YYYYMMDD-XXX
    val company: String,
    val assessorId: String,
    val createdAt: Long,
    val totalAchieved: Int = 0,
    val totalApplicable: Int = 0,
    val percentage: Double = 0.0,
    val isHealthy: Boolean = false,
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED
    val photoPathsJson: String? = null
)
