package com.gamss.android.feature.calendar

import java.time.LocalDate

data class CalendarState(
    val today: LocalDate,
    val selectedDate: LocalDate? = null,
)
