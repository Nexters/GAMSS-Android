package com.gamss.android.feature.calendar.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun DaysOfWeekHeader(
    daysOfWeek: List<DayOfWeek>,
    modifier: Modifier = Modifier,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val dayLabels = remember(daysOfWeek, locale) {
        daysOfWeek.map { dayOfWeek ->
            dayOfWeek.getDisplayName(TextStyle.NARROW, locale) to
                dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        dayLabels.forEach { (narrowLabel, fullLabel) ->
            Text(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = fullLabel },
                text = narrowLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
