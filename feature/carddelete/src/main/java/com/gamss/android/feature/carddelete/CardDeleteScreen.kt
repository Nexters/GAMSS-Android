package com.gamss.android.feature.carddelete

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.feature.carddelete.component.ShredStatusRow
import com.gamss.android.feature.carddelete.component.ShredStrips
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun CardDeleteScreen(
    cardId: Long,
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
        onShredTap = { viewModel.onShredTap(cardId) },
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
                .padding(GamssTheme.spacing.spacing400),
            label = stringResource(
                if (state.isCompleted) R.string.card_delete_complete_button else R.string.card_delete_shred_button,
            ),
            onClick = if (state.isCompleted) onDeleteComplete else onShredTap,
            variant = if (state.isCompleted) GamssButtonVariant.Destructive else GamssButtonVariant.PrimaryDark,
            isProcessing = state.isDeleting,
        )
    }
}

private fun CardDeleteSideEffect.toMessage(context: android.content.Context): String =
    when (this) {
        CardDeleteSideEffect.ShredSuccess -> context.getString(R.string.card_delete_success_message)
        CardDeleteSideEffect.ShredFailure -> context.getString(R.string.card_delete_failure_message)
    }

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
