package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

private val BottomBarHeight = 66.dp
private val ItemRowWidth = 255.dp
private val ItemIconSize = 26.dp
private val ItemLabelGap = 4.dp
private val BottomBarStroke = Color(0xFFDCE0E6)
private val BottomBarStrokeWidth = 1.dp
private val SelectedColor = Color(0xFF303136)
private val UnselectedColor = Color(0xFFAAAFBD)

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
            // 배경과 구분선을 인셋보다 바깥에 둬야 내비게이션 영역까지 흰색이 깔린다.
            .background(Color.White)
            .drawBehind {
                // 화면 폭을 꽉 채우는 바라 좌우·아래 선은 화면 테두리에 붙어 버린다. 위쪽만 긋는다.
                val stroke = BottomBarStrokeWidth.toPx()
                drawRect(color = BottomBarStroke, size = Size(size.width, stroke))
            }
            .navigationBarsPadding()
            .height(BottomBarHeight),
    ) {
        Row(
            modifier = Modifier
                .width(ItemRowWidth)
                .height(44.dp)
                .align(Alignment.Center),
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
    val contentColor = if (selected) SelectedColor else UnselectedColor
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
