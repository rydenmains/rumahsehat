package com.rumahsehat.domain

import com.rumahsehat.data.model.ScoreItem

/**
 * Status 3-tier fixed assessment (17 item / 810 poin) — v1.7.
 *
 * - SEHAT: inti (2.1–2.4 sanitasi) penuh + total ≥ 90% dari total applicable.
 * - TIDAK SEHAT: inti < 80% ATAU total < 70% (mengalahkan KURANG bila overlap).
 * - KURANG SEHAT: sisanya (efektif inti ≥ 80% DAN total ≥ 70%, bukan SEHAT).
 *   KURANG = kreasi sendiri untuk sinyal dini; dokumen resmi Form-03 hanya
 *   mengenal biner SEHAT/TIDAK SEHAT.
 * - Threshold PERSEN dari total applicable → kebal opsi N/A 3.4 (810→790).
 * - Prioritas evaluasi: SEHAT → TIDAK → KURANG.
 */
enum class HealthStatus(val label: String) {
    SEHAT("SEHAT"),
    KURANG_SEHAT("KURANG SEHAT"),
    TIDAK_SEHAT("TIDAK SEHAT");

    companion object {
        fun fromLabel(label: String?): HealthStatus = when (label?.trim()?.uppercase()) {
            "SEHAT" -> SEHAT
            "KURANG SEHAT", "KURANG_SEHAT", "KURANG" -> KURANG_SEHAT
            else -> TIDAK_SEHAT
        }
    }
}

object AssessmentCalculator {

    /*
     * Bobot tiap item = maxScore-nya (lihat FormItemsProvider). Kalkulator hanya
     * mengagregasi; tidak menyimpan tabel bobot sendiri.
     */
    const val ESSENTIAL_MIN_WEIGHT = 100

    /** Item inti = sanitasi 2.1–2.4; SELALU applicable (Q14), max 550. */
    val CORE_ITEM_IDS = setOf("2.1", "2.2", "2.3", "2.4")
    const val CORE_FULL_SCORE = 550

    const val SEHAT_MIN_PERCENT = 90.0
    const val KURANG_CORE_MIN_PERCENT = 80.0
    const val KURANG_TOTAL_MIN_PERCENT = 70.0

    data class CalculationResult(
        val totalAchieved: Int,
        val totalApplicable: Int,
        val percentage: Double,
        val coreAchieved: Int,
        val coreApplicable: Int,
        val corePercentage: Double,
        val status: HealthStatus,
        /** Legacy: true hanya bila SEHAT (kolom isHealthy dipertahankan, Q16). */
        val isHealthy: Boolean,
        val isInvalid: Boolean = false
    )

    fun calculate(items: List<ScoreItem>, weights: Map<String, Int>): CalculationResult {
        var sAchieved = 0
        var sApp = 0
        var cAchieved = 0
        var cApp = 0

        for (item in items) {
            val weight = weights[item.itemId] ?: 0
            if (!item.isApplicable) continue

            sApp += weight
            sAchieved += item.score

            if (item.itemId in CORE_ITEM_IDS) {
                cApp += weight
                cAchieved += item.score
            }
        }

        if (sApp == 0) {
            return CalculationResult(0, 0, 0.0, 0, 0, 0.0,
                HealthStatus.TIDAK_SEHAT, isHealthy = false, isInvalid = true)
        }

        val percentage = (sAchieved.toDouble() / sApp.toDouble()) * 100.0
        val corePercentage = if (cApp > 0) (cAchieved.toDouble() / cApp.toDouble()) * 100.0 else 0.0
        val coreFull = cApp > 0 && cAchieved == cApp

        val status = when {
            coreFull && percentage >= SEHAT_MIN_PERCENT -> HealthStatus.SEHAT
            corePercentage < KURANG_CORE_MIN_PERCENT ||
                percentage < KURANG_TOTAL_MIN_PERCENT -> HealthStatus.TIDAK_SEHAT
            else -> HealthStatus.KURANG_SEHAT
        }
        return CalculationResult(sAchieved, sApp, percentage, cAchieved, cApp,
            corePercentage, status, isHealthy = status == HealthStatus.SEHAT)
    }

    /** Backfill status label untuk baris lama (tanpa subtotal inti tersimpan). */
    fun legacyStatusLabel(isHealthy: Boolean): String =
        if (isHealthy) HealthStatus.SEHAT.label else HealthStatus.TIDAK_SEHAT.label
}