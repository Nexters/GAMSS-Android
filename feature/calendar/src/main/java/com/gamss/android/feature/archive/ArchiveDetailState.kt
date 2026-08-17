package com.gamss.android.feature.archive

import com.gamss.android.domain.card.Card
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
    val isClearDialogVisible: Boolean = false,
    /** 종이를 눌러 날짜별 조회로 받아 온 카드. 상세 팝업이 이 값으로 뜬다. */
    val selectedCard: Card? = null,
    val isCardLoading: Boolean = false,
)
