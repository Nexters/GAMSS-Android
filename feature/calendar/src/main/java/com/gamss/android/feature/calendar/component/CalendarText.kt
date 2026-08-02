package com.gamss.android.feature.calendar.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gamss.android.feature.calendar.R
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun YearMonth.monthTitle(): String = stringResource(
    R.string.calendar_month_title,
    year,
    monthValue,
)

@Composable
internal fun LocalDate.dateDescription(isToday: Boolean): String = stringResource(
    if (isToday) R.string.calendar_date_description_today else R.string.calendar_date_description,
    year,
    monthValue,
    dayOfMonth,
)
