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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationContent
import com.gamss.android.feature.game.R
import com.gamss.android.feature.game.clicker.component.ClickerGaugeNote
import com.gamss.android.feature.game.clicker.component.ClickerGaugeNoteSize
import com.gamss.android.feature.game.clicker.component.ClickerKeyCap
import com.gamss.android.feature.game.clicker.component.ClickerScoreBoard
import com.gamss.android.feature.game.clicker.component.rememberClickerKeySound

private const val KEY_COUNT = 2

@Composable
fun ClickerGameScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = rememberClickerGameController()
    val keySound = rememberClickerKeySound()

    LaunchedEffect(game.isPlaying) {
        if (game.isPlaying) game.runClock()
    }

    ClickerGameContent(
        game = game,
        onBackClick = onBackClick,
        onKeyPress = { if (game.tap()) keySound.play() },
        modifier = modifier,
    )
}

/** 자주 바뀌는 값은 람다로 넘겨 실제로 쓰는 곳에서만 읽는다. */
@Composable
private fun ClickerGameContent(
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
private fun ClickerGamePreview(state: ClickerGameState) {
    GamssTheme(darkTheme = false) {
        ClickerGameContent(game = ClickerGameController(state), onBackClick = {}, onKeyPress = {})
    }
}

@Preview(name = "Ready", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameReadyPreview() {
    ClickerGamePreview(ClickerGameState())
}

@Preview(name = "Playing", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGamePlayingPreview() {
    ClickerGamePreview(
        ClickerGameState(phase = ClickerGamePhase.Playing, score = 232, elapsedMs = 12_300L),
    )
}

@Preview(name = "Small", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameSmallPreview() {
    ClickerGamePreview(ClickerGameState(phase = ClickerGamePhase.Playing, score = 80))
}
