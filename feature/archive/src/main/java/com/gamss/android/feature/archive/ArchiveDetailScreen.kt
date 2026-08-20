package com.gamss.android.feature.archive

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.dialog.GamssDialog
import com.gamss.android.core.designsystem.dialog.GamssDialogAction
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.component.CardDetailDialog
import com.gamss.android.feature.archive.component.ConversationCardDialog
import com.gamss.android.feature.archive.component.MonthSelector
import com.gamss.android.feature.archive.component.PaperPile
import com.gamss.android.feature.archive.component.YearMonthPickerSheet
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.LocalDate
import java.time.YearMonth
import com.gamss.android.core.designsystem.R as DesignSystemR

/**
 * 시스템 뒤로가기는 여기서 BackHandler 로 받지 않는다. 먼저 가로채면 NavDisplay 의 onBack 에 닿지
 * 않아, predictive back 미리보기와 pop 트랜지션이 이 화면에서만 빠진다.
 */
@Composable
fun ArchiveDetailScreen(
    emotion: EmotionCharacter,
    droppedCardId: Long?,
    hasShreddedCard: Boolean,
    onBackClick: () -> Unit,
    onNavigateToCardDelete: (Long?) -> Unit,
    viewModel: ArchiveDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.archive_card_share_chooser_title)
    val conversationLoadFailedMessage = stringResource(R.string.archive_conversation_load_error)

    // 파쇄 화면에서 돌아왔을 때도 다시 받아야 한다. 카드를 지우면 같은 날짜 뒤 순번이 한 칸씩
    // 당겨져, 살아남은 종이가 들고 있던 순번이 서버와 어긋난다.
    LaunchedEffect(emotion) {
        viewModel.load(emotion, force = droppedCardId != null || hasShreddedCard)
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ArchiveDetailSideEffect.OpenCardDelete -> onNavigateToCardDelete(sideEffect.cardId)

            ArchiveDetailSideEffect.ConversationLoadFailed ->
                Toast.makeText(context, conversationLoadFailedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    ArchiveDetailFrame(
        emotion = emotion,
        state = state,
        droppedCardId = droppedCardId,
        onBackClick = onBackClick,
        onPaperClick = viewModel::selectCard,
        onMonthClick = viewModel::showMonthPicker,
        onClearClick = viewModel::showClearDialog,
    )

    ArchiveDetailOverlays(
        emotion = emotion,
        state = state,
        onMonthSelect = viewModel::selectMonth,
        onMonthPickerDismiss = viewModel::dismissMonthPicker,
        onClearConfirm = viewModel::confirmClear,
        onClearDismiss = viewModel::dismissClearDialog,
        onCardDismiss = viewModel::dismissCard,
        onCardDiscard = viewModel::discardSelectedCard,
        onCardConversationClick = viewModel::viewSelectedConversation,
        onCardShare = { card -> shareCard(context, card, shareChooserTitle) },
        onConversationCardDismiss = viewModel::dismissConversationCard,
    )
}

/** 항상 보이는 부분. 위에 겹쳐 뜨는 시트·다이얼로그는 [ArchiveDetailOverlays] 가 맡는다. */
@Composable
private fun ArchiveDetailFrame(
    emotion: EmotionCharacter,
    state: ArchiveDetailState,
    droppedCardId: Long?,
    onBackClick: () -> Unit,
    onPaperClick: (Card) -> Unit,
    onMonthClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Scaffold(
        containerColor = GamssTheme.colors.white,
        topBar = {
            ArchiveDetailTopBar(
                emotion = emotion,
                onBackClick = onBackClick,
                onClearClick = onClearClick,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ArchiveDetailCards(
                cards = state.cards,
                droppedCardId = droppedCardId,
                onPaperClick = onPaperClick,
            )
            // 종이가 쌓일 자리보다 나중에 둔다. 카드가 많아 더미가 위로 넘치면 종이가 셀렉터를
            // 가리고 탭까지 먹어 달을 못 바꾸게 된다.
            MonthSelector(
                yearMonth = state.yearMonth,
                onClick = onMonthClick,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

/** 넷 다 아무것도 안 뜬 상태가 기본이라, 프레임과 떼어 여기서만 켜고 끈다. */
@Composable
private fun ArchiveDetailOverlays(
    emotion: EmotionCharacter,
    state: ArchiveDetailState,
    onMonthSelect: (YearMonth) -> Unit,
    onMonthPickerDismiss: () -> Unit,
    onClearConfirm: () -> Unit,
    onClearDismiss: () -> Unit,
    onCardDismiss: () -> Unit,
    onCardDiscard: () -> Unit,
    onCardConversationClick: () -> Unit,
    onCardShare: (Card) -> Unit,
    onConversationCardDismiss: () -> Unit,
) {
    if (state.isMonthPickerVisible) {
        YearMonthPickerSheet(
            selected = state.yearMonth,
            onSelect = onMonthSelect,
            onDismiss = onMonthPickerDismiss,
        )
    }

    if (state.isClearDialogVisible) {
        ClearConfirmDialog(
            emotionName = emotion.displayName,
            onConfirm = onClearConfirm,
            onDismiss = onClearDismiss,
        )
    }

    state.selectedCard?.let { card ->
        CardDetailDialog(
            card = card,
            onDismiss = onCardDismiss,
            onDiscardClick = onCardDiscard,
            onViewConversationClick = onCardConversationClick,
            onShareClick = { onCardShare(card) },
        )
    }

    // 감정 카드와 같은 자리를 쓰지만 둘이 함께 뜨는 일은 없다.
    state.conversationCard?.let { conversationCard ->
        ConversationCardDialog(
            conversationCard = conversationCard,
            onDismiss = onConversationCardDismiss,
        )
    }
}

private fun shareCard(context: Context, card: Card, chooserTitle: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${card.summary}\n\n${card.message}")
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}

/** 되돌릴 수 없는 삭제라 파쇄 화면으로 넘기기 전에 확인을 한 번 받는다. */
@Composable
private fun ClearConfirmDialog(
    emotionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.archive_clear_dialog_title, emotionName),
        subtitle = stringResource(R.string.archive_clear_dialog_description, emotionName),
        primaryAction = GamssDialogAction(
            label = stringResource(R.string.archive_clear),
            onClick = onConfirm,
            variant = GamssButtonVariant.Destructive,
        ),
        secondaryAction = GamssDialogAction(
            label = stringResource(R.string.archive_clear_dialog_cancel),
            onClick = onDismiss,
            variant = GamssButtonVariant.Secondary,
        ),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ArchiveDetailTopBar(
    emotion: EmotionCharacter,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    GamssTopBar(
        // 비우기는 터치 영역만큼 안쪽 여백을 물고 있어, 오른쪽은 그만큼 뺀다.
        contentPadding = PaddingValues(
            start = TopBarHorizontalPadding,
            end = TopBarHorizontalPadding - ClearHitPadding,
        ),
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GamssIconButton(
                    iconRes = DesignSystemR.drawable.ic_left_chevron,
                    contentDescription = stringResource(R.string.archive_back_description),
                    onClick = onBackClick,
                    hitPadding = BackHitPadding,
                )
                Text(
                    text = emotion.displayName,
                    style = GamssTheme.typography.title4,
                    color = GamssTheme.colors.gray900,
                    modifier = Modifier.padding(start = TitleStartPadding),
                )
            }
        },
        trailing = {
            Text(
                text = stringResource(R.string.archive_clear),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray900,
                modifier = Modifier
                    .clip(RoundedCornerShape(ClearRippleRadius))
                    .clickable(role = Role.Button, onClick = onClearClick)
                    .padding(ClearHitPadding),
            )
        },
    )
}

@Composable
private fun ArchiveDetailCards(
    cards: ArchiveCards,
    droppedCardId: Long?,
    onPaperClick: (Card) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (cards) {
            ArchiveCards.Loading -> CircularProgressIndicator(color = GamssTheme.colors.gray700)
            ArchiveCards.LoadFailed -> EmptyMessage(textRes = R.string.archive_cards_load_failed)
            is ArchiveCards.Loaded -> if (cards.cards.isEmpty()) {
                EmptyMessage(textRes = R.string.archive_cards_empty)
            } else {
                PaperPile(
                    cards = cards.cards,
                    droppedCardId = droppedCardId,
                    onPaperClick = onPaperClick,
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = GamssTheme.typography.body4Medium,
        color = GamssTheme.colors.gray500,
    )
}

private val TopBarHorizontalPadding = 18.dp
private val BackHitPadding = 0.dp
private val TitleStartPadding = 12.dp
private val ClearHitPadding = 8.dp
private val ClearRippleRadius = 8.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ArchiveDetailPaperPilePreview() {
    GamssTheme(darkTheme = false) {
        ArchiveDetailFrame(
            emotion = EmotionCharacter.QUIRKY,
            state = ArchiveDetailState(
                emotion = EmotionCharacter.QUIRKY,
                yearMonth = YearMonth.of(2026, 7),
                cards = ArchiveCards.Loaded(List(24) { index -> PreviewCard.copy(id = index.toLong()) }),
            ),
            droppedCardId = null,
            onBackClick = {},
            onPaperClick = {},
            onMonthClick = {},
            onClearClick = {},
        )
    }
}

private val PreviewCard = Card(
    id = 0L,
    conversationId = 0L,
    character = EmotionCharacter.QUIRKY,
    emotionLabel = "엉뚱",
    summary = "오늘은 좀 엉뚱한 하루였어요.",
    message = "그런 날도 있죠.",
    date = LocalDate.of(2026, 7, 23),
)
