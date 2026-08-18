package com.gamss.android.feature.archive

import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.YearMonth

data class ArchiveDetailState(
    val emotion: EmotionCharacter? = null,
    val yearMonth: YearMonth,
    val cards: ArchiveCards = ArchiveCards.Loading,
    val isMonthPickerVisible: Boolean = false,
    val isClearDialogVisible: Boolean = false,
    val selectedCard: Card? = null,
    val isCardLoading: Boolean = false,
)

/** 종이 더미 자리가 가질 수 있는 상태. 셋이 겹칠 수 없어 플래그 조합 대신 하나로 든다. */
sealed interface ArchiveCards {
    data object Loading : ArchiveCards
    data object LoadFailed : ArchiveCards
    data class Loaded(val entries: List<CardEntry>) : ArchiveCards
}
