package com.gamss.android.feature.calendar

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.GetCardsByDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCardsByDate: GetCardsByDateUseCase,
) :
    ViewModel(),
    ContainerHost<CalendarState, CalendarSideEffect> {

    override val container = container<CalendarState, CalendarSideEffect>(
        CalendarState(today = LocalDate.now()),
    )

    fun selectDate(date: LocalDate) = intent {
        if (date == state.selectedDate) {
            reduce { state.copy(selectedDate = null, cardLoadState = CalendarCardLoadState.Idle) }
            return@intent
        }

        loadCards(date)
    }

    fun retrySelectedDate() = intent {
        state.selectedDate?.let { loadCards(it) }
    }

    private suspend fun Syntax<CalendarState, CalendarSideEffect>.loadCards(date: LocalDate) {
        reduce { state.copy(selectedDate = date, cardLoadState = CalendarCardLoadState.Loading) }
        when (val result = getCardsByDate(date)) {
            is AppResult.Success -> reduce {
                state.copy(
                    cardLoadState = result.data
                        .takeIf { it.isNotEmpty() }
                        ?.let(CalendarCardLoadState::Content)
                        ?: CalendarCardLoadState.Empty,
                )
            }
            is AppResult.Failure -> reduce { state.copy(cardLoadState = CalendarCardLoadState.Error) }
        }
    }
}
