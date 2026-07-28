package com.gamss.android.feature.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.feature.calendar.component.CalendarDayCell
import com.gamss.android.feature.calendar.component.MonthCalendar
import com.kizitonwose.calendar.core.DayPosition
import org.orbitmvi.orbit.compose.collectAsState
import java.time.YearMonth

private const val PAST_MONTH_COUNT = 60L
private const val FUTURE_MONTH_COUNT = 12L

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    val anchorMonth = remember(state.today) { YearMonth.from(state.today) }
    val monthRange = remember(anchorMonth) {
        anchorMonth.minusMonths(PAST_MONTH_COUNT)..anchorMonth.plusMonths(FUTURE_MONTH_COUNT)
    }

    Scaffold { innerPadding ->
        MonthCalendar(
            monthRange = monthRange,
            initialMonth = anchorMonth,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) { day ->
            val isInCurrentMonth = day.position == DayPosition.MonthDate
            CalendarDayCell(
                day = day,
                isSelected = isInCurrentMonth && day.date == state.selectedDate,
                isToday = isInCurrentMonth && day.date == state.today,
                onClick = viewModel::selectDate,
            )
        }
    }
}
