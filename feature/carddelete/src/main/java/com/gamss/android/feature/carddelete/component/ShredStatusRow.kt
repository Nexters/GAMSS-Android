package com.gamss.android.feature.carddelete.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.carddelete.R

@Composable
fun ShredStatusRow(
    isShredding: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusDescription = stringResource(
        if (isShredding) R.string.card_delete_status_shredding else R.string.card_delete_status_ready,
    )
    val bottomBorderColor = GamssTheme.colors.gray400
    val activeColor by animateColorAsState(
        targetValue = if (isShredding) GamssTheme.colors.green else GamssTheme.colors.apricot,
        label = "ShredStatusColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GamssTheme.colors.gray100)
            .semantics { stateDescription = statusDescription }
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = bottomBorderColor,
                    start = Offset(0f, size.height - strokeWidth / 2),
                    end = Offset(size.width, size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth,
                )
            }
            .padding(horizontal = GamssTheme.spacing.spacing400, vertical = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPillToggle(isOn = isShredding)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(StatusIndicatorSize)
                .clip(CircleShape)
                .background(GamssTheme.colors.gray300)
                .border(1.dp, GamssTheme.colors.gray400, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .clip(CircleShape)
                    .background(activeColor),
            )
        }
    }
}

@Composable
private fun StatusPillToggle(isOn: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(GamssTheme.radius.radiusFull))
            .background(GamssTheme.colors.gray300)
            .border(1.dp, GamssTheme.colors.gray400, RoundedCornerShape(GamssTheme.radius.radiusFull))
            .padding(ToggleTrackPadding),
    ) {
        ToggleCell(stringResource(R.string.card_delete_status_off), !isOn, GamssTheme.colors.apricot)
        ToggleCell(stringResource(R.string.card_delete_status_on), isOn, GamssTheme.colors.green)
    }
}

@Composable
private fun ToggleCell(label: String, selected: Boolean, selectedColor: Color) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.Transparent,
        label = "ToggleCellBackground",
    )
    Box(
        modifier = Modifier
            .width(ToggleCellWidth)
            .height(ToggleCellHeight)
            .clip(RoundedCornerShape(GamssTheme.radius.radiusFull))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = GamssTheme.typography.body5Medium,
            color = if (selected) GamssTheme.colors.white else GamssTheme.colors.gray500,
        )
    }
}

private val StatusIndicatorSize = 30.dp
private val StatusDotSize = 18.dp
private val ToggleTrackPadding = 4.dp
private val ToggleCellWidth = 42.dp
private val ToggleCellHeight = 32.dp
