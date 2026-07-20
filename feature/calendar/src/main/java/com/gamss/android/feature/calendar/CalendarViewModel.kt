package com.gamss.android.feature.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor() : ViewModel(),
    ContainerHost<CalendarState, CalendarSideEffect> {

    override val container = container<CalendarState, CalendarSideEffect>(CalendarState())
}