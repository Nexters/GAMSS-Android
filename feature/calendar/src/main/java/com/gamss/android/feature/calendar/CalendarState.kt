package com.gamss.android.feature.calendar

import com.gamss.android.domain.card.Card
import java.time.LocalDate

sealed interface CalendarCardLoadState {
    data object Idle : CalendarCardLoadState
    data object Loading : CalendarCardLoadState
    data object Empty : CalendarCardLoadState
    data class Content(val cards: List<Card>) : CalendarCardLoadState
    data object Error : CalendarCardLoadState
}

/** 카드가 삭제된 뒤의 목록 상태를 계산한다. [Content] 가 아니면 그대로 둔다. */
fun CalendarCardLoadState.withoutCard(cardId: Long): CalendarCardLoadState {
    if (this !is CalendarCardLoadState.Content) return this
    val remaining = cards.filterNot { it.id == cardId }
    return if (remaining.isEmpty()) CalendarCardLoadState.Empty else CalendarCardLoadState.Content(remaining)
}

data class CalendarState(
    val today: LocalDate,
    val selectedDate: LocalDate? = null,
    val cardLoadState: CalendarCardLoadState = CalendarCardLoadState.Idle,
    val selectedCard: Card? = null,
)
