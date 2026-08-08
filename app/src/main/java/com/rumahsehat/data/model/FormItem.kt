package com.rumahsehat.data.model

import kotlin.math.roundToInt

/** Satu pilihan jawaban Kemenkes. `scoreWeight` = pecahan (0..1) dari bobot item. */
data class Option(
    val letter: Char,
    val label: String,
    val scoreWeight: Float
)

data class FormItem(
    val id: String,
    val titleRes: Int,
    val maxScore: Int,
    val options: List<Option>,
    var selectedOptionIndex: Int = -1,
    var isApplicable: Boolean = true,
    var reason: String? = null
) {
    val selectedOption: Option?
        get() = options.getOrNull(selectedOptionIndex)

    fun scoreForOption(index: Int): Int =
        (maxScore * options[index].scoreWeight).roundToInt()

    /** Skor diambil dari opsi terpilih; item tidak berlaku = 0. */
    val currentScore: Int
        get() {
            if (!isApplicable || selectedOptionIndex < 0) return 0
            return scoreForOption(selectedOptionIndex)
        }
}