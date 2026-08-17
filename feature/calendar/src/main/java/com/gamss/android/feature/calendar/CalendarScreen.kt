package com.gamss.android.feature.calendar

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.card.GamssEmotionCardContent
import com.gamss.android.core.designsystem.card.GamssImageCard
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.feature.calendar.component.CalendarDayCell
import com.gamss.android.feature.calendar.component.CardDetailDialog
import com.gamss.android.feature.calendar.component.MonthCalendar
import com.kizitonwose.calendar.core.DayPosition
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val PAST_MONTH_COUNT = 60L
private const val FUTURE_MONTH_COUNT = 12L

@Composable
fun CalendarScreen(
    onOpenConversation: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.calendar_card_share_chooser_title)
    val discardFailureMessage = stringResource(R.string.calendar_card_discard_failure)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is CalendarSideEffect.OpenChatRoom -> onOpenConversation(sideEffect.conversationId)
            CalendarSideEffect.CardDiscardFailed ->
                Toast.makeText(context, discardFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val anchorMonth = remember(state.today) { YearMonth.from(state.today) }
    val monthRange = remember(anchorMonth) {
        anchorMonth.minusMonths(PAST_MONTH_COUNT)..anchorMonth.plusMonths(FUTURE_MONTH_COUNT)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            MonthCalendar(
                monthRange = monthRange,
                initialMonth = anchorMonth,
            ) { day ->
                val isInCurrentMonth = day.position == DayPosition.MonthDate
                CalendarDayCell(
                    day = day,
                    isSelected = isInCurrentMonth && day.date == state.selectedDate,
                    isToday = isInCurrentMonth && day.date == state.today,
                    onClick = viewModel::selectDate,
                )
            }
            CalendarCardResults(
                selectedDate = state.selectedDate,
                loadState = state.cardLoadState,
                onRetryClick = viewModel::retrySelectedDate,
                onCardClick = viewModel::selectCard,
                modifier = Modifier.weight(1f),
            )
        }
    }

    state.selectedCard?.let { card ->
        CardDetailDialog(
            card = card,
            onDismiss = viewModel::dismissCardDetail,
            onDiscardClick = viewModel::discardSelectedCard,
            onViewConversationClick = viewModel::viewSelectedConversation,
            onShareClick = { shareCard(context, card, shareChooserTitle) },
        )
    }
}

private fun shareCard(context: Context, card: Card, chooserTitle: String) {
    val shareText = "${card.summary}\n\n${card.message}"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}

@Composable
private fun CalendarCardResults(
    selectedDate: LocalDate?,
    loadState: CalendarCardLoadState,
    onRetryClick: () -> Unit,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedDate == null) return

    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = stringResource(
                R.string.calendar_card_result_title,
                selectedDate.monthValue,
                selectedDate.dayOfMonth,
            ),
            style = GamssTheme.typography.title3,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (loadState) {
            CalendarCardLoadState.Idle -> Unit
            CalendarCardLoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            CalendarCardLoadState.Empty -> Text(
                text = stringResource(R.string.calendar_card_empty),
                style = GamssTheme.typography.body4Regular,
                color = GamssTheme.colors.gray600,
            )
            CalendarCardLoadState.Error -> {
                Text(
                    text = stringResource(R.string.calendar_card_load_error),
                    style = GamssTheme.typography.body4Regular,
                    color = GamssTheme.colors.gray600,
                )
                TextButton(onClick = onRetryClick) {
                    Text(text = stringResource(R.string.calendar_card_retry))
                }
            }
            is CalendarCardLoadState.Content -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(loadState.cards, key = Card::id) { card ->
                    CalendarCardItem(card, onClick = { onCardClick(card) })
                }
            }
        }
    }
}

@Composable
private fun CalendarCardItem(card: Card, onClick: () -> Unit) {
    val detailDescription = stringResource(R.string.calendar_card_detail_description)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = detailDescription, onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        GamssImageCard(date = card.date.format(CardDateFormatter)) {
            GamssEmotionCardContent(
                character = card.character.toGamssEmotionCardCharacter(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = card.emotionLabel,
                        modifier = Modifier.fillMaxWidth(),
                        style = GamssTheme.typography.subtitle3,
                        color = GamssTheme.colors.gray800,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        text = card.summary,
                        modifier = Modifier.fillMaxWidth(),
                        style = GamssTheme.typography.title3,
                        color = GamssTheme.colors.gray950,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val CardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM.dd")
