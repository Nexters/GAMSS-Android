package com.gamss.android.feature.emotion

import com.gamss.android.domain.emotion.EmotionResult

data class EmotionState(
    val input: String = "",
    val isRunning: Boolean = false,
    val result: EmotionResult? = null,
    val notRecognized: Boolean = false,
    val error: String? = null,
)
