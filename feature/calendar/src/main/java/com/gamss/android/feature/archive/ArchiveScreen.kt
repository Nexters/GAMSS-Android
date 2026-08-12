package com.gamss.android.feature.archive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

private val archiveItems = listOf(
    ArchiveItem("분노", R.drawable.worry_bin_anger_body, R.drawable.worry_bin_anger_note, 0f),
    ArchiveItem("기쁨", R.drawable.worry_bin_joy_body, R.drawable.worry_bin_joy_note, 45f),
    ArchiveItem("슬픔", R.drawable.worry_bin_sad_body, R.drawable.worry_bin_sad_note, -15f),
    ArchiveItem("까칠", R.drawable.worry_bin_prickly_body, R.drawable.worry_bin_prickly_note, -7f),
    ArchiveItem("불안", R.drawable.worry_bin_anxiety_body, R.drawable.worry_bin_anxiety_note, 11f),
    ArchiveItem("엉뚱", R.drawable.worry_bin_quirky_body, R.drawable.worry_bin_quirky_note, -12f),
)

@Composable
fun ArchiveScreen(
    onNavigateToSetting: () -> Unit,
) {
    Scaffold(
        containerColor = GamssTheme.colors.white,
        topBar = { ArchiveTopBar(onMenuClick = onNavigateToSetting) },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 32.dp,
                end = 32.dp,
                top = 28.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "다시 보고 싶은 쓰레기통을 열어보세요",
                    modifier = Modifier.fillMaxWidth(),
                    style = GamssTheme.typography.body4Medium,
                    color = GamssTheme.colors.gray950,
                )
            }
            items(items = archiveItems, key = { it.title }) { item ->
                ArchiveCard(item = item)
            }
        }
    }
}

@Composable
private fun ArchiveTopBar(
    onMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "GAMSS",
            style = GamssTheme.typography.title4.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive,
            ),
            color = GamssTheme.colors.gray950,
        )
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "설정",
                tint = GamssTheme.colors.gray700,
            )
        }
    }
}

@Composable
private fun ArchiveCard(
    item: ArchiveItem,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(width = 140.dp, height = 164.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(item.bodyRes),
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentScale = ContentScale.FillWidth,
        )
        Text(
            text = item.title,
            modifier = Modifier.padding(top = 16.dp),
            style = GamssTheme.typography.title5,
            color = GamssTheme.colors.gray900,
        )
        Box(
            modifier = Modifier
                .padding(top = 52.dp)
                .size(width = 60.dp, height = 58.dp)
                .rotate(item.noteRotation),
        ) {
            Image(
                painter = painterResource(item.noteRes),
                contentDescription = null,
                modifier = Modifier.size(width = 60.dp, height = 58.dp),
            )
        }
    }
}

private data class ArchiveItem(
    val title: String,
    @param:DrawableRes val bodyRes: Int,
    @param:DrawableRes val noteRes: Int,
    val noteRotation: Float,
)
