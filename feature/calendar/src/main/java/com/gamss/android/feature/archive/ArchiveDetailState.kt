package com.gamss.android.feature.archive

import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter

data class ArchiveDetailState(
    val emotion: EmotionCharacter? = null,
    val isLoading: Boolean = true,
    val cards: List<Card> = emptyList(),
    val loadFailed: Boolean = false,
)
