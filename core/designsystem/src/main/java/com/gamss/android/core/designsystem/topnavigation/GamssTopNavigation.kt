package com.gamss.android.core.designsystem.topnavigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * Figma 사양(node-id=559-210):
 * - 기본적으로 상단 Safety area(상태바)와 6dp 띄어서 배치됩니다.
 * - [backgroundColor]를 화면 배경색과 동일하게 맞춰 사용하는 경우, [ignoresSafeArea]를 true로 설정해
 *   배경이 상태바 뒤까지 이어지도록 해야 이음새 없이 자연스럽습니다.
 *
 * 주의: [ignoresSafeArea] = true로 배경을 상태바 뒤까지 확장하는 경우, 상태바 아이콘(시계/배터리 등)의
 * 명암 대비는 이 컴포넌트가 자동으로 맞춰주지 않습니다. 상태바 아이콘 색은 Activity/Window 전역 상태라
 * 재사용되는 leaf 컴포넌트가 SideEffect로 임의 변경하면 화면 간 충돌·미리보기 오작동 위험이 있어
 * 의도적으로 다루지 않았습니다. [backgroundColor]가 어두우면 해당 화면(Activity)에서 직접
 * `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars`를 false로,
 * 밝으면 true로 화면 진입 시 한 번 설정해 주세요.
 */
@Composable
fun GamssTopNavigation(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = if (isSystemInDarkTheme()) GamssTheme.colors.black else GamssTheme.colors.white,
    ignoresSafeArea: Boolean = false,
    showLeftIcon: Boolean = false,
    showRightIcon: Boolean = false,
    onLeftIconClick: () -> Unit = {},
    onRightIconClick: () -> Unit = {},
) {
    val safeAreaModifier = if (ignoresSafeArea) {
        // 배경을 상태바 뒤까지 먼저 그린 뒤, 콘텐츠만 상태바 아래로 내립니다.
        Modifier
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    } else {
        // 상태바 인셋 + 6dp 여백을 먼저 확보한 뒤에만 배경을 그립니다.
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 6.dp)
            .background(backgroundColor)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(safeAreaModifier)
            .height(64.dp)
            .padding(horizontal = 18.dp, vertical = GamssTheme.spacing.spacing200),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopNavigationIconSlot(
            visible = showLeftIcon,
            iconRes = R.drawable.ic_left_chevron,
            contentDescription = null,
            onClick = onLeftIconClick,
        )

        Text(
            text = title,
            style = GamssTheme.typography.subtitle2,
            color = GamssTheme.colors.gray900,
        )

        TopNavigationIconSlot(
            visible = showRightIcon,
            iconRes = R.drawable.ic_right_chevron,
            contentDescription = null,
            onClick = onRightIconClick,
        )
    }
}

/**
 * 아이콘이 보이지 않을 때도 24dp 슬롯 자체는 항상 차지하도록 유지합니다.
 * 그래야 좌우 슬롯 폭이 항상 동일해 [Arrangement.SpaceBetween] 안에서 title이 흔들리지 않고
 * 가운데 고정됩니다.
 */
@Composable
private fun TopNavigationIconSlot(
    visible: Boolean,
    iconRes: Int,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = GamssTheme.colors.gray900,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun GamssTopNavigationLightPreview() {
    GamssTheme {
        GamssTopNavigation(title = "Title", showLeftIcon = true, showRightIcon = true)
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GamssTopNavigationDarkPreview() {
    GamssTheme {
        GamssTopNavigation(title = "Title")
    }
}
