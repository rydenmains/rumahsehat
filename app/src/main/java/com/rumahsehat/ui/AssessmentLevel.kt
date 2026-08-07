package com.rumahsehat.ui

import com.rumahsehat.R

/*
 * Tingkat kelengkapan item ditampilkan sebagai teks, bukan angka,
 * sesuai ketentuan "skor tidak ditampilkan ke petugas lapangan".
 */
object AssessmentLevel {
    fun labelRes(score: Int, maxScore: Int): Int {
        if (maxScore <= 0) return R.string.level_none
        val ratio = score.toDouble() / maxScore
        return when {
            ratio >= 1.0 -> R.string.level_excellent
            ratio >= 0.75 -> R.string.level_good
            ratio >= 0.5 -> R.string.level_fair
            ratio >= 0.25 -> R.string.level_poor
            else -> R.string.level_none
        }
    }
}