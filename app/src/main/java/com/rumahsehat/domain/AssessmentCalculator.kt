package com.rumahsehat.domain

import com.rumahsehat.data.model.ScoreItem

object AssessmentCalculator {

    /*
     * Rumah sehat dinilai dari formulir: bobot setiap item adalah maxScore-nya
     * (lihat FormItemsProvider). Kalkulator hanya mengagregasi; tidak menyimpan
     * tabel bobot sendiri, jadi form dan kalkulator tidak mungkin melenceng.
     *
     * Aturan status: SEHAT hanya bila semua item esensial (bobot besar) terisi
     * penuh. Item esensial = bobot >= ESSENTIAL_MIN_WEIGHT.
     */
    const val ESSENTIAL_MIN_WEIGHT = 100

    data class CalculationResult(
        val totalAchieved: Int,
        val totalApplicable: Int,
        val percentage: Double,
        val isHealthy: Boolean,
        val isInvalid: Boolean = false
    )

    fun calculate(items: List<ScoreItem>, weights: Map<String, Int>): CalculationResult {
        var sAchieved = 0
        var sApp = 0
        var essentialFailed = false

        for (item in items) {
            val weight = weights[item.itemId] ?: 0
            if (!item.isApplicable) continue

            sApp += weight
            sAchieved += item.score

            if (weight >= ESSENTIAL_MIN_WEIGHT && item.score < weight) {
                essentialFailed = true
            }
        }

        if (sApp == 0) {
            return CalculationResult(0, 0, 0.0, false, isInvalid = true)
        }

        val percentage = (sAchieved.toDouble() / sApp.toDouble()) * 100.0
        return CalculationResult(sAchieved, sApp, percentage, isHealthy = !essentialFailed)
    }
}