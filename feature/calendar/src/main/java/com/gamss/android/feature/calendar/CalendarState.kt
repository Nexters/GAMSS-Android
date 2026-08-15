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

data class CalendarState(
    val today: LocalDate,
    val selectedDate: LocalDate? = null,
    val cardLoadState: CalendarCardLoadState = CalendarCardLoadState.Idle,
)
