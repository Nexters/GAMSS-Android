package com.gamss.android.feature.game.clicker.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.game.R

private const val TENTHS_PER_SECOND = 10

@Composable
internal fun ClickerScoreBoard(
    remainingTenths: Long,
    score: Int,
    onReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledIcon(
            iconRes = R.drawable.game_ic_clock,
            label = stringResource(
                R.string.game_clicker_remaining_time,
                remainingTenths / TENTHS_PER_SECOND,
                remainingTenths % TENTHS_PER_SECOND,
            ),
            iconLabelGap = 3.dp,
        )
        Row(
            modifier = Modifier
                .widthIn(min = ScoreBoxWidth)
                .background(GamssTheme.colors.white)
                .border(ScoreBoxBorderWidth, GamssTheme.colors.gray950)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Text(
                text = stringResource(R.string.game_clicker_score, score),
                style = GamssTheme.typography.title2,
                color = GamssTheme.colors.gray900,
            )
            Text(
                text = stringResource(R.string.game_clicker_score_unit),
                style = GamssTheme.typography.title2,
                color = GamssTheme.colors.gray900,
            )
        }
        LabeledIcon(
            iconRes = R.drawable.game_ic_replay,
            label = stringResource(R.string.game_clicker_replay),
            iconLabelGap = 2.dp,
            modifier = Modifier.clickable(role = Role.Button, onClick = onReplayClick),
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
private val ScoreBoxWidth = 142.dp
private val ScoreBoxBorderWidth = 1.5.dp
