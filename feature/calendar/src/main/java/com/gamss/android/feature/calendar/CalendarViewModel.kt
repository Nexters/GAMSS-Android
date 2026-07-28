package com.gamss.android.feature.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor() :
    ViewModel(),
    ContainerHost<CalendarState, CalendarSideEffect> {

    override val container = container<CalendarState, CalendarSideEffect>(
        CalendarState(today = LocalDate.now()),
    )

    fun selectDate(date: LocalDate) = intent {
        val nextSelectedDate = date.takeIf { it != state.selectedDate }
        reduce { state.copy(selectedDate = nextSelectedDate) }
    }
}
