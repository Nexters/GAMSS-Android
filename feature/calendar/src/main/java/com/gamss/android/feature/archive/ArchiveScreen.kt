package com.gamss.android.feature.archive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssLogo
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.emotion.EmotionCharacter

private val archiveItems = listOf(
    ArchiveItem(EmotionCharacter.ANGER, R.drawable.worry_bin_anger),
    ArchiveItem(EmotionCharacter.JOY, R.drawable.worry_bin_joy),
    ArchiveItem(EmotionCharacter.SADNESS, R.drawable.worry_bin_sad),
    ArchiveItem(EmotionCharacter.PRICKLY, R.drawable.worry_bin_prickly),
    ArchiveItem(EmotionCharacter.ANXIETY, R.drawable.worry_bin_anxiety),
    ArchiveItem(EmotionCharacter.QUIRKY, R.drawable.worry_bin_quirky),
)

@Composable
fun ArchiveScreen(
    onNavigateToSetting: () -> Unit,
    onArchiveClick: (EmotionCharacter) -> Unit,
) {
    Scaffold(
        containerColor = GamssTheme.colors.white,
        topBar = { ArchiveTopBar(onMenuClick = onNavigateToSetting) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val scale = (maxWidth / ArchiveDesignWidth).coerceAtMost(1f)
            val gridWidth = ArchiveDesignWidth * scale

            Column(
                modifier = Modifier
                    .width(gridWidth)
                    .padding(
                        start = ArchiveHorizontalPadding * scale,
                        end = ArchiveHorizontalPadding * scale,
                        top = ArchiveTopPadding * scale,
                        bottom = ArchiveBottomPadding * scale,
                    ),
                verticalArrangement = Arrangement.spacedBy(ArchiveVerticalSpacing * scale),
            ) {
                archiveItems.chunked(ARCHIVE_COLUMN_COUNT).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ArchiveHorizontalSpacing * scale),
                    ) {
                        rowItems.forEach { item ->
                            ArchiveCard(item = item, scale = scale, onClick = { onArchiveClick(item.emotion) })
                        }
                    }
                }
            }
            Text(
                text = "다시 보고 싶은 쓰레기통을 열어보세요",
                modifier = Modifier
                    .width(gridWidth)
                    .padding(top = ArchiveTitleTopPadding * scale),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray950,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ArchiveTopBar(
    onMenuClick: () -> Unit,
) {
    GamssTopBar(
        contentPadding = PaddingValues(start = ArchiveTopBarStartPadding, end = ArchiveTopBarEndPadding),
        leading = { GamssLogo(contentDescription = stringResource(R.string.archive_logo_description)) },
        trailing = {
            GamssIconButton(
                iconRes = GamssIcons.Setting,
                contentDescription = "설정",
                onClick = onMenuClick,
            )
        },
    )
}

@Composable
private fun ArchiveCard(
    item: ArchiveItem,
    scale: Float,
    onClick: () -> Unit,
) {
    Image(
        painter = painterResource(item.binRes),
        contentDescription = stringResource(R.string.archive_open_description, item.emotion.displayName),
        modifier = Modifier
            .size(width = ArchiveCardWidth * scale, height = ArchiveCardHeight * scale)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

private data class ArchiveItem(
    val emotion: EmotionCharacter,
    @param:DrawableRes val binRes: Int,
)

private val ArchiveDesignWidth = 402.dp
private val ArchiveHorizontalPadding = 48.dp
private val ArchiveTopPadding = 32.dp
private val ArchiveBottomPadding = 27.dp
private val ArchiveTitleTopPadding = 8.dp
private val ArchiveHorizontalSpacing = 40.dp
private val ArchiveVerticalSpacing = 11.dp
private val ArchiveCardWidth = 132.dp
private val ArchiveCardHeight = 172.dp
private val ArchiveTopBarStartPadding = 20.dp
private val ArchiveTopBarEndPadding = 8.dp
private const val ARCHIVE_COLUMN_COUNT = 2
