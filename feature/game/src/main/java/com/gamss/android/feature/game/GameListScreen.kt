package com.gamss.android.feature.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssLogo
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme

@Composable
fun GameListScreen(
    onNavigateToSetting: () -> Unit,
    onClickerGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        GamssTopBar(
            contentPadding = PaddingValues(start = TopBarStartPadding, end = TopBarEndPadding),
            leading = { GamssLogo(contentDescription = stringResource(R.string.game_logo_description)) },
            trailing = {
                GamssIconButton(
                    iconRes = GamssIcons.Setting,
                    contentDescription = stringResource(R.string.game_setting_description),
                    onClick = onNavigateToSetting,
                )
            },
        )
        GameCard(
            onClick = onClickerGameClick,
            modifier = Modifier.padding(start = CardStartPadding, top = CardTopPadding),
        )
    }
}

@Composable
private fun GameCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(CardWidth)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CardLabelGap),
    ) {
        Image(
            painter = painterResource(R.drawable.game_clicker_thumbnail),
            contentDescription = null,
            modifier = Modifier.size(width = CardWidth, height = ThumbnailHeight),
        )
        Text(
            text = stringResource(R.string.game_clicker_title),
            style = GamssTheme.typography.body3Medium,
            color = GamssTheme.colors.gray950,
            textAlign = TextAlign.Center,
        )
    }
}

private val TopBarStartPadding = 20.dp
private val TopBarEndPadding = 8.dp
private val CardStartPadding = 24.dp

// 시안의 상단바(64dp)보다 GamssTopBar(62dp)가 2dp 낮아 그만큼 더한다.
private val CardTopPadding = 33.dp

private val CardWidth = 157.dp
private val ThumbnailHeight = 143.67.dp
private val CardLabelGap = 12.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun GameListScreenPreview() {
    GamssTheme(darkTheme = false) {
        GameListScreen(onNavigateToSetting = {}, onClickerGameClick = {})
    }
}
