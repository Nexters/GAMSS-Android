package com.gamss.android.feature.game.clicker.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.game.R

private const val TENTHS_PER_SECOND = 10

@Composable
internal fun ClickerScoreBoard(
    remainingTenths: () -> Long,
    score: () -> Int,
    onReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemainingTime(remainingTenths = remainingTenths)
        ScoreBox(score = score)
        LabeledIcon(
            iconRes = R.drawable.game_ic_replay,
            label = stringResource(R.string.game_clicker_replay),
            iconLabelGap = 2.dp,
            modifier = Modifier.clickable(role = Role.Button, onClick = onReplayClick),
        )
    }
}

/** 0.1초마다 바뀌므로 이 칸만 다시 그려지도록 따로 둔다. */
@Composable
private fun RemainingTime(remainingTenths: () -> Long) {
    val tenths = remainingTenths()
    LabeledIcon(
        iconRes = R.drawable.game_ic_clock,
        label = stringResource(
            R.string.game_clicker_remaining_time,
            tenths / TENTHS_PER_SECOND,
            tenths % TENTHS_PER_SECOND,
        ),
        iconLabelGap = 3.dp,
    )
}

@Composable
private fun ScoreBox(score: () -> Int) {
    val value = score()
    val description = stringResource(R.string.game_clicker_score_description, value)
    Row(
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = description }
            .widthIn(min = ScoreBoxSize.width)
            .height(ScoreBoxSize.height)
            .paint(painterResource(R.drawable.game_clicker_score_box), contentScale = ContentScale.FillBounds)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.game_clicker_score, value),
            style = GamssTheme.typography.title2,
            color = GamssTheme.colors.gray900,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.game_clicker_score_unit),
            style = GamssTheme.typography.title2,
            color = GamssTheme.colors.gray900,
            maxLines = 1,
        )
    }
}

@Composable
private fun LabeledIcon(
    @DrawableRes iconRes: Int,
    label: String,
    iconLabelGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(min = LabeledIconWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(iconLabelGap),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = GamssTheme.colors.gray900,
        )
        Text(
            text = label,
            style = GamssTheme.typography.subtitle3,
            color = GamssTheme.colors.gray900,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private val ItemGap = 36.dp
private val LabeledIconWidth = 62.dp

// 손그림 테두리가 시안 프레임(142x59)보다 사방으로 약 1dp 넘친다. 글자가 길면 가로로 늘린다.
private val ScoreBoxSize = DpSize(144.33.dp, 61.33.dp)
