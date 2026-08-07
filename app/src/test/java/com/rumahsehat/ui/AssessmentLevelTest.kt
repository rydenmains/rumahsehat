package com.rumahsehat.ui

import com.rumahsehat.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentLevelTest {

    private val max = 20

    @Test
    fun `skor penuh - excellent`() {
        assertEquals(R.string.level_excellent, AssessmentLevel.labelRes(max, max))
    }

    @Test
    fun `tiga perempat - good`() {
        assertEquals(R.string.level_good, AssessmentLevel.labelRes(15, max))
    }

    @Test
    fun `setengah - fair`() {
        assertEquals(R.string.level_fair, AssessmentLevel.labelRes(10, max))
    }

    @Test
    fun `seperempat - poor`() {
        assertEquals(R.string.level_poor, AssessmentLevel.labelRes(5, max))
    }

    @Test
    fun `nol - none`() {
        assertEquals(R.string.level_none, AssessmentLevel.labelRes(0, max))
    }

    @Test
    fun `maxScore tidak valid - none`() {
        assertEquals(R.string.level_none, AssessmentLevel.labelRes(20, 0))
    }
}