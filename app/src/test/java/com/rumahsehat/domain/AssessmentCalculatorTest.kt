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
        assertEquals(HealthStatus.SEHAT, result.status)
        assertTrue(result.isHealthy)
        assertFalse(result.isInvalid)
    }

    @Test
    fun `inti tidak penuh tapi di atas 80 persen - kurang sehat`() {
        // 2.1 opsi b (75/150) → inti 475/550 = 86,4% → KURANG; total 735/810 = 90,7%.
        val items = allFull().map {
            if (it.itemId == "2.1") it.copy(score = 75) else it
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertEquals(HealthStatus.KURANG_SEHAT, result.status)
        assertFalse(result.isHealthy)
        assertFalse(result.isInvalid)
    }

    @Test
    fun `inti jatuh di bawah 80 persen - tidak sehat walau total di atas 70`() {
        // 2.1 = 0 → inti 400/550 = 72,7% → TIDAK (walau total 660/810 = 81,5%).
        val items = allFull().map {
            if (it.itemId == "2.1") it.copy(score = 0) else it
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertEquals(HealthStatus.TIDAK_SEHAT, result.status)
        assertFalse(result.isHealthy)
    }

    @Test
    fun `total di bawah 70 persen - tidak sehat walau inti penuh`() {
        // Inti penuh (550) tapi semua non-inti 0 → total 550/810 = 67,9% → TIDAK.
        val items = allFull().map {
            if (it.itemId !in AssessmentCalculator.CORE_ITEM_IDS) it.copy(score = 0) else it
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertEquals(HealthStatus.TIDAK_SEHAT, result.status)
        assertFalse(result.isHealthy)
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
        assertEquals(HealthStatus.TIDAK_SEHAT, result.status)
    }

    @Test
    fun `opsi N/A 3.4 - threshold persen kebal, inti penuh 90 persen tetap sehat`() {
        // 3.4 N/A → total 790/790 = 100% + inti penuh → SEHAT.
        val result = AssessmentCalculator.calculate(allFull(nonApplicable = setOf("3.4")), weights)
        assertEquals(100.0, result.percentage, 0.01)
        assertEquals(HealthStatus.SEHAT, result.status)
    }

    @Test
    fun `item esensial tidak berlaku - sisa penuh tetap sehat`() {
        val result = AssessmentCalculator.calculate(allFull(nonApplicable = setOf("2.1")), weights)
        assertEquals(100.0, result.percentage, 0.01)
        assertTrue(result.isHealthy)
    }

    @Test
    fun `boundary sehat - inti penuh dan total pas 90 persen`() {
        // Non-inti hilang 81 poin (810 → 729 = 90,0%): 1.1..1.4 = b (10+10+10+10=40),
        // 1.5 = a (0, hilang 20), 3.5 = a (0, hilang 20)... total hilang 80 → 730/810 = 90,1%.
        // Pakai: 1.1..1.4 opsi b (skor 10 tiap item, hilang 40) + 1.5 opsi a (hilang 20)
        // + 3.5 opsi a (hilang 20) = hilang 80 → 730/810 ≥ 90% + inti penuh → SEHAT.
        val items = allFull().map {
            when (it.itemId) {
                "1.1", "1.2", "1.3", "1.4" -> it.copy(score = 10)
                "1.5", "3.5" -> it.copy(score = 0)
                else -> it
            }
        }
        val result = AssessmentCalculator.calculate(items, weights)
        assertTrue(result.percentage >= 90.0)
        assertEquals(HealthStatus.SEHAT, result.status)
    }
}