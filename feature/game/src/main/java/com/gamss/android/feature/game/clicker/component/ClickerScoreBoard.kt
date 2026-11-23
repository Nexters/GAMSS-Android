package com.gamss.android.feature.game.clicker.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.game.R
import com.gamss.android.feature.game.clicker.ClickerRecord

private const val TENTHS_PER_SECOND = 10
private const val HUNDREDTHS_PER_SECOND = 100

/** 가운데 칸이 시안처럼 화면 가운데 오도록 다시하기와 같은 폭을 왼쪽에 비워 둔다. */
@Composable
internal fun ClickerRecordBoard(
    record: () -> ClickerRecord,
    onReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(SideItemWidth))
        RecordBox(record = record)
        ReplayButton(onClick = onReplayClick)
    }
}

/** 0.1초마다 바뀌므로 이 칸만 다시 그려지도록 따로 둔다. */
@Composable
private fun RecordBox(record: () -> ClickerRecord) {
    val text = when (val value = record()) {
        is ClickerRecord.Running -> stringResource(
            R.string.game_clicker_elapsed_time,
            value.tenths / TENTHS_PER_SECOND,
            value.tenths % TENTHS_PER_SECOND,
        )
        is ClickerRecord.Cleared -> stringResource(
            R.string.game_clicker_record,
            value.hundredths / HUNDREDTHS_PER_SECOND,
            value.hundredths % HUNDREDTHS_PER_SECOND,
        )
        ClickerRecord.TimeOver -> stringResource(R.string.game_clicker_time_over)
    }
    Text(
        text = text,
        modifier = Modifier
            .widthIn(min = RecordBoxWidth)
            .background(GamssTheme.colors.white)
            .border(RecordBoxBorderWidth, GamssTheme.colors.gray950)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        style = GamssTheme.typography.title2,
        color = GamssTheme.colors.gray900,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun ReplayButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = SideItemWidth)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.game_ic_replay),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = GamssTheme.colors.gray900,
        )
        Text(
            text = stringResource(R.string.game_clicker_replay),
            style = GamssTheme.typography.subtitle3,
            color = GamssTheme.colors.gray900,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private val ItemGap = 36.dp
private val SideItemWidth = 62.dp
private val RecordBoxWidth = 142.dp
private val RecordBoxBorderWidth = 1.5.dp
