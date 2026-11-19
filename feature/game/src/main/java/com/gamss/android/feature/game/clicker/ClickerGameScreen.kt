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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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

// 화면이 가려지면 프레임이 멈춘다. 돌아왔을 때 그 공백을 한 프레임으로 몰아 받으면 시간이
// 한꺼번에 흘러가 버리므로, 한 프레임에 반영하는 시간을 제한해 가려진 동안은 멈춘 것으로 친다.
private const val MAX_FRAME_DELTA_MS = 100L
private const val KEY_COUNT = 2

private val ClickerGameStateSaver = listSaver<ClickerGameState, Any>(
    save = { listOf(it.phase.name, it.score, it.gauge, it.elapsedMs) },
    restore = {
        ClickerGameState(
            phase = ClickerGamePhase.valueOf(it[0] as String),
            score = it[1] as Int,
            gauge = it[2] as Float,
            elapsedMs = it[3] as Long,
        )
    },
)

@Composable
fun ClickerGameScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var game by rememberSaveable(stateSaver = ClickerGameStateSaver) { mutableStateOf(ClickerGameState()) }
    val keySound = rememberClickerKeySound()

    LaunchedEffect(game.phase) {
        if (game.phase != ClickerGamePhase.Playing) return@LaunchedEffect
        var lastFrameMs = withFrameMillis { it }
        while (true) {
            val frameMs = withFrameMillis { it }
            game = game.advance((frameMs - lastFrameMs).coerceAtMost(MAX_FRAME_DELTA_MS))
            lastFrameMs = frameMs
        }
    }

    ClickerGameContent(
        game = game,
        onBackClick = onBackClick,
        onKeyPress = {
            val next = game.tap()
            if (next != game) keySound.play()
            game = next
        },
        onReplayClick = { game = ClickerGameState() },
        modifier = modifier,
    )
}

@Composable
private fun ClickerGameContent(
    game: ClickerGameState,
    onBackClick: () -> Unit,
    onKeyPress: () -> Unit,
    onReplayClick: () -> Unit,
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
            remainingTenths = game.remainingTenths,
            score = game.score,
            onReplayClick = onReplayClick,
            modifier = Modifier.padding(top = ScoreBoardTopPadding),
        )
        GaugeArea(
            gauge = game.gauge,
            isIdle = game.phase == ClickerGamePhase.Ready,
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
    gauge: Float,
    isIdle: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(gauge, 0f..1f) },
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
                isIdle = isIdle,
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

@Preview(name = "Ready", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameReadyPreview() {
    GamssTheme(darkTheme = false) {
        ClickerGameContent(game = ClickerGameState(), onBackClick = {}, onKeyPress = {}, onReplayClick = {})
    }
}

@Preview(name = "Playing", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGamePlayingPreview() {
    GamssTheme(darkTheme = false) {
        ClickerGameContent(
            game = ClickerGameState(phase = ClickerGamePhase.Playing, score = 232, gauge = 0.5f, elapsedMs = 12_300L),
            onBackClick = {},
            onKeyPress = {},
            onReplayClick = {},
        )
    }
}

@Preview(name = "Small", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerGameSmallPreview() {
    GamssTheme(darkTheme = false) {
        ClickerGameContent(
            game = ClickerGameState(phase = ClickerGamePhase.Playing, score = 80, gauge = 0.3f),
            onBackClick = {},
            onKeyPress = {},
            onReplayClick = {},
        )
    }
}
