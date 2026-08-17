package com.gamss.android.feature.archive

import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.YearMonth

data class ArchiveDetailState(
    val emotion: EmotionCharacter? = null,
    val yearMonth: YearMonth,
    val isLoading: Boolean = true,
    val cards: List<CardEntry> = emptyList(),
    val loadFailed: Boolean = false,
    val isMonthPickerVisible: Boolean = false,
)
