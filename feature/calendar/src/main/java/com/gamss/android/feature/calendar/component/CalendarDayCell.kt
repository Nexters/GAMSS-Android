package com.gamss.android.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import java.time.LocalDate

private const val OUT_DATE_ALPHA = 0.4f

@Composable
internal fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInCurrentMonth = day.position == DayPosition.MonthDate
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isInCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = OUT_DATE_ALPHA)
    }
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        )
    } else {
        Modifier
    }
    val dayDescription = day.date.dateDescription(isToday)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(borderModifier)
            .selectable(
                selected = isSelected,
                enabled = isInCurrentMonth,
                onClick = { onClick(day.date) },
            )
            .semantics(mergeDescendants = true) { contentDescription = dayDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )
    }
}
