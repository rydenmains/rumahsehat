package com.rumahsehat.data.model

data class FormItem(
    val id: String,
    val titleRes: Int,
    val maxScore: Int,
    var currentScore: Int = 0,
    var isApplicable: Boolean = true,
    var reason: String? = null
)
