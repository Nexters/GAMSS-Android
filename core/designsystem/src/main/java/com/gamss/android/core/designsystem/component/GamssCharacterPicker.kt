package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssRadius
import com.gamss.android.core.designsystem.theme.GamssTheme

// Figma 노드 3331:3950 의 auto layout 값. 패널 211x94(hug), 패딩 20, 3x2 그리드, 간격 18.
// 셀 폭 45 는 그 값에서 역산한 것이다: (211 - 40 - 36) / 3.
private val PickerPadding = 20.dp
private val PickerGap = 18.dp
private val ItemWidth = 45.dp

// 높이는 역산값 18 을 최소치로만 쓴다. caption2 의 lineHeight 가 마침 18sp 라, 고정하면
// 시스템 글꼴을 키웠을 때 글자가 잘린다.
private val ItemMinHeight = 18.dp
private val IndicatorSize = 18.dp
private val IndicatorLabelGap = 2.dp
private val PanelBorderWidth = 1.dp
private const val COLUMNS = 3

/** 반응할 캐릭터를 고르는 패널. 한 줄에 [COLUMNS] 개씩 끊어 놓는다. */
@Composable
fun <T> GamssCharacterPicker(
    items: List<GamssCharacterPickerItem<T>>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GamssRadius.radius050))
            .background(GamssTheme.colors.white)
            .border(PanelBorderWidth, GamssTheme.colors.gray200, RoundedCornerShape(GamssRadius.radius050))
            .padding(PickerPadding),
        verticalArrangement = Arrangement.spacedBy(PickerGap),
    ) {
        items.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PickerGap)) {
                row.forEach { item ->
                    CharacterPickerItem(item = item, onToggle = onToggle)
                }
            }
        }
    }
}

@Composable
private fun <T> CharacterPickerItem(
    item: GamssCharacterPickerItem<T>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .width(ItemWidth)
            .defaultMinSize(minHeight = ItemMinHeight)
            .toggleable(
                value = item.selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle(item.value) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IndicatorLabelGap),
    ) {
        Image(
            painter = painterResource(if (item.selected) GamssIcons.CheckCircleOn else GamssIcons.CheckCircleOff),
            contentDescription = null,
            modifier = Modifier.size(IndicatorSize),
        )
        GamssText(
            text = item.label,
            style = GamssTheme.typography.caption2,
            // 아이콘 색과 맞춘다. 선택은 gray700, 미선택은 gray500.
            color = if (item.selected) GamssTheme.colors.gray700 else GamssTheme.colors.gray500,
            maxLines = 1,
        )
    }
}
