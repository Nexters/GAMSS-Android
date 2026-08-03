package com.gamss.android.feature.calendar.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth

private val CalendarFirstDayOfWeek = DayOfWeek.SUNDAY

@Composable
fun MonthCalendar(
    monthRange: ClosedRange<YearMonth>,
    initialMonth: YearMonth,
    modifier: Modifier = Modifier,
    dayContent: @Composable BoxScope.(CalendarDay) -> Unit,
) {
    val monthPagerState = rememberCalendarState(
        startMonth = monthRange.start,
        endMonth = monthRange.endInclusive,
        firstVisibleMonth = initialMonth,
        firstDayOfWeek = CalendarFirstDayOfWeek,
    )
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = CalendarFirstDayOfWeek) }
    val visibleMonth = monthPagerState.firstVisibleMonth.yearMonth

    var targetMonth by remember(monthPagerState) { mutableStateOf(visibleMonth) }
    LaunchedEffect(monthPagerState) {
        snapshotFlow { monthPagerState.isScrollInProgress to monthPagerState.firstVisibleMonth.yearMonth }
            .collect { (isScrollInProgress, month) ->
                if (!isScrollInProgress) targetMonth = month
            }
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollToMonth = remember(monthPagerState, monthRange, coroutineScope) {
        fun(month: YearMonth) {
            val target = month.coerceIn(monthRange)
            targetMonth = target
            coroutineScope.launch { monthPagerState.animateScrollToMonth(target) }
        }
    }

    Column(modifier = modifier) {
        CalendarMonthNavigator(
            currentMonth = visibleMonth,
            monthRange = monthRange,
            onPreviousMonthClick = { scrollToMonth(targetMonth.minusMonths(1)) },
            onNextMonthClick = { scrollToMonth(targetMonth.plusMonths(1)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        DaysOfWeekHeader(daysOfWeek = daysOfWeek)
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalCalendar(
            state = monthPagerState,
            dayContent = dayContent,
        )
    }
}
