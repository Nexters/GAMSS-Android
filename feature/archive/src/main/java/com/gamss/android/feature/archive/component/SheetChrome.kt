package com.gamss.android.feature.archive.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 아카이브의 바텀시트들이 함께 쓰는 모양값과 상단 닫기 버튼.
 *
 * 시트가 둘 이상이 되면서 같은 여백을 각자 들고 있을 이유가 없어져 여기로 모았다.
 */

internal val SheetCornerRadius = 20.dp
internal val SheetTopPadding = 24.dp
internal val SheetHorizontalPadding = 20.dp
internal val SheetHitPadding = 12.dp

private val SheetBottomPadding = 32.dp
private val MinGapAboveSystemBar = 8.dp

/**
 * 제스처 바(24dp)는 디자인의 홈 인디케이터처럼 여백 위에 겹쳐도 된다. 3버튼 내비처럼 인셋이 디자인
 * 여백보다 크면 버튼이 내비바에 붙으므로, 그때는 인셋 위로 최소 간격만큼 띄운다.
 */
@Composable
internal fun sheetBottomPadding(): Dp {
    val systemBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return SheetBottomPadding.coerceAtLeast(systemBarInset + MinGapAboveSystemBar)
}

@Composable
internal fun SheetCloseButton(contentDescription: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // hitPadding 만큼 당겨, 아이콘 자체가 디자인 위치에 오게 한다.
            .padding(top = SheetTopPadding - SheetHitPadding, end = SheetHorizontalPadding - SheetHitPadding),
        horizontalArrangement = Arrangement.End,
    ) {
        GamssIconButton(
            iconRes = GamssIcons.Close,
            contentDescription = contentDescription,
            onClick = onClick,
            hitPadding = SheetHitPadding,
            tint = GamssTheme.colors.gray400,
        )
    }
}
