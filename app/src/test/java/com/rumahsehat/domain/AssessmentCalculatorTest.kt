package com.rumahsehat.domain

import com.rumahsehat.data.model.ScoreItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentCalculatorTest {

    // Bobot mengikuti FormItemsProvider (satu-satunya sumber kebenaran).
    private val weights = mapOf(
        "1.1" to 20, "1.2" to 20, "1.3" to 20, "1.4" to 20, "1.5" to 20,
        "1.6" to 20, "1.7" to 20, "1.8" to 20,
        "2.1" to 150, "2.2" to 150, "2.3" to 100, "2.4" to 150,
        "3.1" to 20, "3.2" to 20, "3.3" to 20, "3.4" to 20, "3.5" to 20
    )

    private fun allFull(nonApplicable: Set<String> = emptySet()) =
        weights.map { (id, w) ->
            ScoreItem(assessmentId = "t", itemId = id, score = w, isApplicable = id !in nonApplicable)
        }

    @Test
    fun `semua terisi penuh - sehat 100 persen`() {
        val result = AssessmentCalculator.calculate(allFull(), weights)
        assertEquals(100.0, result.percentage, 0.01)
        assertTrue(result.isHealthy)
        assertFalse(result.isInvalid)
    }

    @Test
    fun `item esensial tidak penuh - tidak sehat`() {
        val items = allFull().map {
            if (it.itemId == "2.1") it.copy(score = 100) else it
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertTrue(result.percentage < 100.0)
        assertFalse(result.isHealthy)
    }

    @Test
    fun `item non-esensial gagal - persen turun tapi tetap sehat`() {
        val items = allFull().map {
            if (it.itemId == "1.1") it.copy(score = 0) else it
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertTrue(result.percentage < 100.0)
        assertTrue(result.isHealthy)
    }

    @Test
    fun `tidak ada item berlaku - invalid`() {
        val items = listOf(
            ScoreItem(assessmentId = "t", itemId = "1.1", score = 0, isApplicable = false)
        )
        val result = AssessmentCalculator.calculate(items, weights)
        assertTrue(result.isInvalid)
    }

    @Test
    fun `item esensial tidak berlaku - sisa penuh tetap sehat`() {
        val result = AssessmentCalculator.calculate(allFull(nonApplicable = setOf("2.1")), weights)
        assertEquals(100.0, result.percentage, 0.01)
        assertTrue(result.isHealthy)
    }
}