package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

// 손그림 테두리는 path 조각이 500여 개라 VectorDrawable 로 넣으면 aapt 문자열 한도를 넘겨 STRING_TOO_LARGE 로 잘린다. 배경만 3배율 PNG 로 둔다.
// 높이는 디자인 62dp 에 에셋의 stroke 여백 2dp 를 더한 값.
private val BottomBarHeight = 64.dp
private val BottomBarHorizontalMargin = 20.dp
private val BottomBarBottomMargin = 8.dp
private val ItemRowHorizontalPadding = 53.dp
private val ItemIconSize = 24.dp
private val ItemLabelGap = 4.dp

/** 화면이 edge-to-edge 라 시스템 내비게이션 영역은 이 컴포넌트가 직접 피한다. */
@Composable
fun <T> GamssBottomBar(
    items: List<GamssBottomBarItem<T>>,
    selectedValue: T,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = BottomBarHorizontalMargin, vertical = BottomBarBottomMargin)
            .height(BottomBarHeight),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_tab_bar),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ItemRowHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = item.value == selectedValue,
                    onClick = { onItemClick(item.value) },
                )
            }
        }
    }
}

@Composable
private fun <T> BottomBarItem(
    item: GamssBottomBarItem<T>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) GamssTheme.colors.gray900 else GamssTheme.colors.gray300
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ItemLabelGap),
    ) {
        Box(
            modifier = Modifier.size(ItemIconSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = contentColor,
            )
        }
        GamssText(
            text = item.label,
            style = GamssTheme.typography.caption3,
            color = contentColor,
        )
    }
}
