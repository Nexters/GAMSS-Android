package com.gamss.android.feature.archive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.gamss.android.core.designsystem.theme.designScale
import com.gamss.android.core.designsystem.theme.designWidth
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
        // 상태바는 GamssTopBar 가, 내비게이션 바는 MainScreen 의 탭바가 각자 피한다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            val scale = designScale(maxWidth)

            // 타이틀과 그리드를 한 Column 으로 묶어야 가운데 정렬이 그룹 전체에 걸린다.
            Column(modifier = Modifier.width(designWidth(scale))) {
                Text(
                    text = stringResource(R.string.archive_grid_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TitleTopPadding * scale),
                    style = GamssTheme.typography.body4Medium,
                    color = GamssTheme.colors.gray950,
                    textAlign = TextAlign.Center,
                )
                ArchiveGrid(scale = scale, onArchiveClick = onArchiveClick)
            }
        }
    }
}

@Composable
private fun ArchiveGrid(
    scale: Float,
    onArchiveClick: (EmotionCharacter) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            start = GridHorizontalPadding * scale,
            end = GridHorizontalPadding * scale,
            top = GridTopPadding * scale,
            bottom = GridBottomPadding * scale,
        ),
        verticalArrangement = Arrangement.spacedBy(GridVerticalSpacing * scale),
    ) {
        archiveItems.chunked(GRID_COLUMN_COUNT).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                // 카드 두 장과 간격을 합쳐도 가용 폭보다 2dp 모자란다(디자인도 그렇다). 그 여분을
                // 한쪽에 몰지 않고 좌우로 나눠야 그리드가 실제로 가운데 온다.
                horizontalArrangement = Arrangement.spacedBy(
                    space = GridHorizontalSpacing * scale,
                    alignment = Alignment.CenterHorizontally,
                ),
            ) {
                rowItems.forEach { item ->
                    ArchiveCard(item = item, scale = scale, onClick = { onArchiveClick(item.emotion) })
                }
            }
        }
    }
}

@Composable
private fun ArchiveTopBar(onMenuClick: () -> Unit) {
    GamssTopBar(
        contentPadding = PaddingValues(start = TopBarStartPadding, end = TopBarEndPadding),
        leading = { GamssLogo(contentDescription = stringResource(R.string.archive_logo_description)) },
        trailing = {
            GamssIconButton(
                iconRes = GamssIcons.Setting,
                contentDescription = stringResource(R.string.archive_setting_description),
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
    val interactionSource = remember { MutableInteractionSource() }
    Image(
        painter = painterResource(item.binRes),
        contentDescription = stringResource(R.string.archive_open_description, item.emotion.displayName),
        modifier = Modifier
            .size(width = CardWidth * scale, height = CardHeight * scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    )
}

private data class ArchiveItem(
    val emotion: EmotionCharacter,
    @param:DrawableRes val binRes: Int,
)

private const val GRID_COLUMN_COUNT = 2

private val TitleTopPadding = 8.dp
private val GridHorizontalPadding = 48.dp
private val GridTopPadding = 32.dp
private val GridBottomPadding = 27.dp
private val GridHorizontalSpacing = 40.dp
private val GridVerticalSpacing = 11.dp
private val CardWidth = 132.dp
private val CardHeight = 172.dp
private val TopBarStartPadding = 20.dp
private val TopBarEndPadding = 8.dp
