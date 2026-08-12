package com.gamss.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 아이콘 하나짜리 버튼.
 *
 * [hitPadding] 은 clickable 안쪽에 들어가므로 아이콘 크기는 그대로 두고 터치 영역만 넓힌다.
 * 기본값 기준 터치 영역은 48dp 다.
 *
 * 아이콘 크기는 VectorDrawable 에 박힌 값을 그대로 쓴다. Figma 내보내기는 stroke 여백까지 담아 크기가
 * 정해져 있어, 일괄로 24dp 에 맞추면 아이콘마다 비율이 틀어진다.
 */
@Composable
fun GamssIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hitPadding: Dp = 12.dp,
    tint: Color = GamssTheme.colors.gray700,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(hitPadding),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
