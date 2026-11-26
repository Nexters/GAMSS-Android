package com.gamss.android.feature.game.clicker.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.feature.game.R

/** 화면만 어둡게 덮고 입력은 막지 않는다. 카운트다운 중의 키 입력은 게임 상태가 무시한다. */
@Composable
internal fun ClickerCountdownOverlay(
    second: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClickerScrimColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(countdownDrawable(second)),
            contentDescription = stringResource(R.string.game_clicker_countdown_description, second),
            modifier = Modifier
                .size(CountdownDigitSize)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@DrawableRes
private fun countdownDrawable(second: Int): Int = when (second) {
    3 -> R.drawable.game_clicker_countdown_3
    2 -> R.drawable.game_clicker_countdown_2
    else -> R.drawable.game_clicker_countdown_1
}

internal val ClickerScrimColor = Color.Black.copy(alpha = 0.6f)
private val CountdownDigitSize = 120.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ClickerCountdownOverlayPreview() {
    ClickerCountdownOverlay(second = 3)
}
