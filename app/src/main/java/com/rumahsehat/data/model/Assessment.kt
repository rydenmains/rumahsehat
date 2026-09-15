package com.rumahsehat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey
    val id: String, // ASM-YYYYMMDD-XXX
    val company: String,
    val assessorId: String,
    /** Nama pemilik / alamat rumah yang diperiksa (halaman-0 identitas). */
    val houseName: String = "",
    val createdAt: Long,
    val totalAchieved: Int = 0,
    val totalApplicable: Int = 0,
    val percentage: Double = 0.0,
    val isHealthy: Boolean = false,
    /** Status 3-tier v1.7: SEHAT | KURANG SEHAT | TIDAK SEHAT. isHealthy dipertahankan (Q16). */
    val status: String = "TIDAK SEHAT",
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED
    val photoPathsJson: String? = null
)
