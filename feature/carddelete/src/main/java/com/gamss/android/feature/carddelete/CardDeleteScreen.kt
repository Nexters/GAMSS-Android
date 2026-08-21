package com.gamss.android.feature.carddelete

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.carddelete.component.ShredStatusRow
import com.gamss.android.feature.carddelete.component.ShredStrips
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/** @param cardId 파쇄할 카드. null 이면 [emotion] 칸을 통째로 파쇄한다. */
@Composable
fun CardDeleteScreen(
    emotion: EmotionCharacter,
    cardId: Long?,
    onBackClick: () -> Unit,
    onDeleteComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardDeleteViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = state.isDeleting || state.isCompleted) {
        if (state.isCompleted) onDeleteComplete()
    }

    viewModel.collectSideEffect { sideEffect ->
        Toast.makeText(context, sideEffect.toMessage(context), Toast.LENGTH_SHORT).show()
    }

    CardDeleteContent(
        state = state,
        onBackClick = when {
            state.isDeleting -> ({ Unit })
            state.isCompleted -> onDeleteComplete
            else -> onBackClick
        },
        onDeleteComplete = onDeleteComplete,
        onShredTap = { viewModel.onShredTap(emotion, cardId) },
        modifier = modifier,
    )
}

/**
 * 확인 다이얼로그 없이, 버튼을 누를 때마다([onShredTap]) 종이가 한 단계씩 내려간다 — 연타 자체가
 * 확인 절차를 대신한다. 끝까지 내려가면 ViewModel이 그 시점에 실제 삭제를 요청한다.
 */
@Composable
private fun CardDeleteContent(
    state: CardDeleteState,
    onBackClick: () -> Unit,
    onDeleteComplete: () -> Unit,
    onShredTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.gray025),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GamssTopNavigation(
                title = stringResource(R.string.card_delete_title),
                showLeftIcon = true,
                onLeftIconClick = onBackClick,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                ShredStrips(
                    progress = state.shredProgress,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = ShredStatusRowHeight - ShredStripOverlayHeight),
                )
                ShredStatusRow(
                    isShredding = state.isShredding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ShredStatusRowHeight),
                )
            }
        }

        GamssButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = GamssTheme.spacing.spacing400,
                    end = GamssTheme.spacing.spacing400,
                    top = GamssTheme.spacing.spacing400,
                    bottom = buttonBottomPadding(),
                ),
            label = stringResource(
                if (state.isCompleted) R.string.card_delete_complete_button else R.string.card_delete_shred_button,
            ),
            onClick = if (state.isCompleted) onDeleteComplete else onShredTap,
            variant = if (state.isCompleted) GamssButtonVariant.Destructive else GamssButtonVariant.PrimaryDark,
            isProcessing = state.isDeleting,
        )
    }
}

/**
 * 제스처 바(24dp)는 디자인 여백 위에 겹쳐도 된다. 3버튼 내비처럼 인셋이 디자인 여백보다 크면 파쇄
 * 버튼이 내비바에 가리므로, 그때는 인셋 위로 최소 간격만큼 띄운다.
 *
 * 이 화면은 하단바가 없는 상세 화면이라 시스템 내비게이션 바를 피해 줄 주체가 따로 없다 —
 * MainScreen 의 NavDisplay 는 상태바만 처리하고, 내비바는 GamssBottomBar 가 직접 피한다.
 */
@Composable
private fun buttonBottomPadding(): Dp {
    val systemBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return GamssTheme.spacing.spacing400.coerceAtLeast(systemBarInset + MinGapAboveSystemBar)
}

private fun CardDeleteSideEffect.toMessage(context: android.content.Context): String =
    when (this) {
        CardDeleteSideEffect.ShredSuccess -> context.getString(R.string.card_delete_success_message)
        CardDeleteSideEffect.ShredFailure -> context.getString(R.string.card_delete_failure_message)
    }

private val MinGapAboveSystemBar = 8.dp
private val ShredStatusRowHeight = 90.dp
private val ShredStripOverlayHeight = 4.dp

@Preview(name = "Idle", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun CardDeleteContentIdlePreview() {
    GamssTheme(darkTheme = false) {
        CardDeleteContent(state = CardDeleteState(), onBackClick = {}, onDeleteComplete = {}, onShredTap = {})
    }
}
