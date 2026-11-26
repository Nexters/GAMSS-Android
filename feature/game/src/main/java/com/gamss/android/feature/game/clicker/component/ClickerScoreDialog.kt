package com.gamss.android.feature.game.clicker.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.game.CLICKER_TOP_SCORE_COUNT
import com.gamss.android.domain.game.ClickerScoreRecord
import com.gamss.android.feature.game.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 뒤의 게임 화면이 눌리지 않도록 덮개가 입력을 소비한다. */
@Composable
internal fun ClickerScoreDialog(
    topScores: List<ClickerScoreRecord>,
    onReplayClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClickerScrimColor)
            .clickable(indication = null, interactionSource = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = CardHorizontalMargin)
                .widthIn(max = CardMaxWidth)
                .fillMaxWidth()
                .paint(painterResource(R.drawable.game_clicker_score_paper), contentScale = ContentScale.FillBounds)
                .padding(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.game_clicker_score_title),
                style = GamssTheme.typography.subtitle2,
                color = GamssTheme.colors.gray900,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            DashedDivider(modifier = Modifier.padding(top = 23.dp, bottom = 18.dp))
            ScoreRows(topScores = topScores)
            DashedDivider(modifier = Modifier.padding(top = 23.dp, bottom = 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamssButton(
                    label = stringResource(R.string.game_clicker_replay),
                    onClick = onReplayClick,
                    variant = GamssButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                GamssButton(
                    label = stringResource(R.string.game_clicker_score_exit),
                    onClick = onExitClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 기록이 10개보다 적어도 버튼 위치가 흔들리지 않도록 최소 10줄 높이를 잡아 둔다. 큰 글꼴에서는 늘어난다. */
@Composable
private fun ScoreRows(topScores: List<ClickerScoreRecord>) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern(DATE_PATTERN) }
    val zone = remember { ZoneId.systemDefault() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight * CLICKER_TOP_SCORE_COUNT + RowGap * (CLICKER_TOP_SCORE_COUNT - 1)),
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        topScores.forEachIndexed { index, record ->
            ScoreRow(
                rank = (index + 1).toString(),
                score = stringResource(R.string.game_clicker_score_description, record.score),
                date = dateFormatter.format(Instant.ofEpochMilli(record.playedAtMillis).atZone(zone)),
            )
        }
    }
}

@Composable
private fun ScoreRow(
    rank: String,
    score: String,
    date: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreCell(text = rank, modifier = Modifier.width(RankWidth))
        ScoreCell(text = score, modifier = Modifier.weight(1f))
        ScoreCell(text = date, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ScoreCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = GamssTheme.typography.body4Regular,
        color = GamssTheme.colors.gray800,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    val color = GamssTheme.colors.gray300
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val dash = DashLength.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
    }
}

private const val DATE_PATTERN = "yyyy.MM.dd"
private val CardHorizontalMargin = 18.dp
private val CardMaxWidth = 366.dp
private val RowHeight = 20.dp
private val RowGap = 8.dp
private val RankWidth = 40.dp
private val ColumnGap = 12.dp
private val DashLength = 5.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerScoreDialogPreview() {
    GamssTheme(darkTheme = false) {
        ClickerScoreDialog(
            topScores = List(7) { ClickerScoreRecord(score = 300 - it * 20, playedAtMillis = 1_764_000_000_000L) },
            onReplayClick = {},
            onExitClick = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerScoreDialogEmptyPreview() {
    GamssTheme(darkTheme = false) {
        ClickerScoreDialog(topScores = emptyList(), onReplayClick = {}, onExitClick = {})
    }
}
