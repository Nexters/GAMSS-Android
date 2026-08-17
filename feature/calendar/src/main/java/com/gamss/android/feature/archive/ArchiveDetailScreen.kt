package com.gamss.android.feature.archive

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
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.component.MonthSelector
import com.gamss.android.feature.archive.component.PaperPile
import com.gamss.android.feature.archive.component.YearMonthPickerSheet
import org.orbitmvi.orbit.compose.collectAsState
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
    onBackClick: () -> Unit,
    viewModel: ArchiveDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    LaunchedEffect(emotion) { viewModel.load(emotion) }

    ArchiveDetailFrame(
        emotion = emotion,
        state = state,
        onBackClick = onBackClick,
        onMonthClick = viewModel::showMonthPicker,
        onMonthSelect = viewModel::selectMonth,
        onMonthPickerDismiss = viewModel::dismissMonthPicker,
        onClearClick = viewModel::showClearDialog,
        // 삭제 API 가 붙기 전이라 확인도 닫기만 한다.
        onClearConfirm = viewModel::dismissClearDialog,
        onClearDismiss = viewModel::dismissClearDialog,
    )
}

@Composable
private fun ArchiveDetailFrame(
    emotion: EmotionCharacter,
    state: ArchiveDetailState,
    onBackClick: () -> Unit,
    onMonthClick: () -> Unit,
    onMonthSelect: (YearMonth) -> Unit,
    onMonthPickerDismiss: () -> Unit,
    onClearClick: () -> Unit,
    onClearConfirm: () -> Unit,
    onClearDismiss: () -> Unit,
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
            MonthSelector(
                yearMonth = state.yearMonth,
                onClick = onMonthClick,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ArchiveDetailCards(state = state)
        }
    }

    if (state.isMonthPickerVisible) {
        YearMonthPickerSheet(
            selected = state.yearMonth,
            onSelect = onMonthSelect,
            onDismiss = onMonthPickerDismiss,
        )
    }

    if (state.isClearDialogVisible) {
        ClearConfirmDialog(onConfirm = onClearConfirm, onDismiss = onClearDismiss)
    }
}

/** 되돌릴 수 없는 삭제라 확인을 한 번 받는다. 무엇을 지울지는 [onConfirm] 을 넘기는 쪽이 정한다. */
@Composable
private fun ClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.archive_clear_dialog_title),
        subtitle = stringResource(R.string.archive_clear_dialog_description),
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
private fun ArchiveDetailCards(state: ArchiveDetailState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator(color = GamssTheme.colors.gray700)
            state.loadFailed -> EmptyMessage(textRes = R.string.archive_cards_load_failed)
            state.cards.isEmpty() -> EmptyMessage(textRes = R.string.archive_cards_empty)
            else -> PaperPile(cards = state.cards)
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
                isLoading = false,
                cards = List(24) { index -> PreviewCard.copy(indexInDate = index) },
            ),
            onBackClick = {},
            onMonthClick = {},
            onMonthSelect = {},
            onMonthPickerDismiss = {},
            onClearClick = {},
            onClearConfirm = {},
            onClearDismiss = {},
        )
    }
}

private val PreviewCard = CardEntry(
    date = LocalDate.of(2026, 7, 23),
    indexInDate = 0,
    character = EmotionCharacter.QUIRKY,
)
