package com.gamss.android.feature.archive

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

private val archiveItems = listOf(
    ArchiveItem("분노", R.drawable.worry_bin_anger),
    ArchiveItem("기쁨", R.drawable.worry_bin_joy),
    ArchiveItem("슬픔", R.drawable.worry_bin_sad),
    ArchiveItem("까칠", R.drawable.worry_bin_prickly),
    ArchiveItem("불안", R.drawable.worry_bin_anxiety),
    ArchiveItem("엉뚱", R.drawable.worry_bin_quirky),
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
            contentPadding = PaddingValues(
                start = 48.dp,
                end = 48.dp,
                top = 32.dp,
                bottom = 27.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "다시 보고 싶은 쓰레기통을 열어보세요",
                    modifier = Modifier.padding(bottom = 23.dp),
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
            .padding(horizontal = 18.dp, vertical = 19.dp),
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
    Image(
        painter = painterResource(item.binRes),
        contentDescription = item.title,
        modifier = Modifier.size(width = 132.dp, height = 172.dp),
    )
}

private data class ArchiveItem(
    val title: String,
    @param:DrawableRes val binRes: Int,
)
