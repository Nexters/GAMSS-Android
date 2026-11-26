package com.gamss.android.feature.game.clicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationContent
import com.gamss.android.domain.game.ClickerScoreRecord
import com.gamss.android.feature.game.R
import com.gamss.android.feature.game.clicker.component.ClickerCountdownOverlay
import com.gamss.android.feature.game.clicker.component.ClickerGaugeNote
import com.gamss.android.feature.game.clicker.component.ClickerGaugeNoteSize
import com.gamss.android.feature.game.clicker.component.ClickerKeyCap
import com.gamss.android.feature.game.clicker.component.ClickerScoreBoard
import com.gamss.android.feature.game.clicker.component.ClickerScoreDialog
import com.gamss.android.feature.game.clicker.component.rememberClickerKeySound
import org.orbitmvi.orbit.compose.collectAsState

private const val KEY_COUNT = 2

@Composable
fun ClickerGameScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClickerGameViewModel = hiltViewModel(),
) {
    val game = rememberClickerGameController()
    val scoreState by viewModel.collectAsState()
    val keySound = rememberClickerKeySound()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(game.isFinished) {
        if (!game.isFinished) game.runClock(onFinished = viewModel::onGameFinished)
    }

    ClickerGameContent(
        game = game,
        topScores = scoreState.topScores,
        onBackClick = onBackClick,
        // 소리와 진동은 점수에 반영된 입력에만 낸다.
        onKeyPress = {
            if (game.tap()) {
                keySound.play()
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ClickerGameContent(
    game: ClickerGameController,
    topScores: List<ClickerScoreRecord>,
    onBackClick: () -> Unit,
    onKeyPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 팝업이 떠 있으면 TalkBack 포커스가 뒤의 게임 화면으로 넘어가지 않게 한다.
        ClickerGamePlayArea(
            game = game,
            onBackClick = onBackClick,
            onKeyPress = onKeyPress,
            modifier = if (game.isFinished) Modifier.clearAndSetSemantics {} else Modifier,
        )
        game.countdownSecond?.let { second -> ClickerCountdownOverlay(second = second) }
        if (game.isFinished) {
            ClickerScoreDialog(topScores = topScores, onReplayClick = game::replay, onExitClick = onBackClick)
        }
    }
}

/** 자주 바뀌는 값은 람다로 넘겨 실제로 쓰는 곳에서만 읽는다. */
@Composable
private fun ClickerGamePlayArea(
    game: ClickerGameController,
    onBackClick: () -> Unit,
    onKeyPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GamssTopNavigation(
            content = GamssTopNavigationContent.None,
            backgroundColor = Color.Transparent,
            showLeftIcon = true,
            leftIconContentDescription = stringResource(R.string.game_clicker_back_description),
            onLeftIconClick = onBackClick,
        )
        ClickerScoreBoard(
            remainingTenths = { game.remainingTenths },
            score = { game.score },
            onReplayClick = game::replay,
            modifier = Modifier.padding(top = ScoreBoardTopPadding),
        )
        GaugeArea(
            gauge = { game.gauge },
            gaugeProgress = { game.gaugeProgress },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        KeyRow(
            onKeyPress = onKeyPress,
            modifier = Modifier.padding(top = KeyRowTopPadding, bottom = KeyRowBottomPadding),
        )
    }
}

/** 시안보다 좁거나 낮은 화면에서는 메모지를 통째로 줄인다. 넓은 화면에서 키우면 손그림이 뭉개진다. */
@Composable
private fun GaugeArea(
    gauge: () -> Float,
    gaugeProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val progress = gaugeProgress()
    BoxWithConstraints(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        },
        contentAlignment = Alignment.BottomCenter,
    ) {
        val scale = minOf(
            1f,
            maxWidth / (ClickerGaugeNoteSize.width + GaugeNoteHorizontalMargin * 2),
            maxHeight / ClickerGaugeNoteSize.height,
        )
        Box(modifier = Modifier.size(ClickerGaugeNoteSize * scale)) {
            ClickerGaugeNote(
                gauge = gauge,
                modifier = Modifier
                    .requiredSize(ClickerGaugeNoteSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

@Composable
private fun KeyRow(
    onKeyPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyDescription = stringResource(R.string.game_clicker_key_description)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KeyRowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally),
    ) {
        repeat(KEY_COUNT) {
            ClickerKeyCap(
                onPress = onKeyPress,
                contentDescription = keyDescription,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = KeyWidth)
                    .aspectRatio(KEY_ASPECT_RATIO),
            )
        }
    }
}

private val ScoreBoardTopPadding = 30.dp
private val GaugeNoteHorizontalMargin = 18.dp
private val KeyRowTopPadding = 14.dp
private val KeyRowBottomPadding = 42.dp
private val KeyRowHorizontalPadding = 18.dp
private val KeyWidth = 140.dp
private val KeyGap = 32.dp
private const val KEY_ASPECT_RATIO = 167f / 179f

@Composable
private fun ClickerGamePreview(state: ClickerGameState, topScores: List<ClickerScoreRecord> = emptyList()) {
    GamssTheme(darkTheme = false) {
        ClickerGameContent(
            game = ClickerGameController(state),
            topScores = topScores,
            onBackClick = {},
            onKeyPress = {},
        )
    }
}

@Preview(name = "Countdown", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameCountdownPreview() {
    ClickerGamePreview(ClickerGameState())
}

@Preview(name = "Playing", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGamePlayingPreview() {
    ClickerGamePreview(ClickerGameState.playing(score = 232, playedMs = 12_300L))
}

@Preview(name = "Finished", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameFinishedPreview() {
    ClickerGamePreview(
        ClickerGameState.finished(score = 232),
        topScores = listOf(ClickerScoreRecord(score = 232, playedAtMillis = 1_764_000_000_000L)),
    )
}

@Preview(name = "Small", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameSmallPreview() {
    ClickerGamePreview(ClickerGameState.playing(score = 80))
}
