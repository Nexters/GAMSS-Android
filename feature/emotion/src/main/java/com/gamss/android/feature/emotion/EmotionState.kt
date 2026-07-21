package com.gamss.android.feature.emotion

import com.gamss.android.domain.emotion.ConversationEmotion

data class EmotionState(
    val input: String = "",
    val isRunning: Boolean = false,
    val result: ConversationEmotion? = null,
    val notRecognized: Boolean = false,
    val error: String? = null,
)
