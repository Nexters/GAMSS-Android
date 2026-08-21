package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.gamssShadow
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 목록 위에 떠서 맨 아래로 데려가는 둥근 버튼. 채팅방과 카드 안 대화가 같은 버튼을 쓴다.
 *
 * @param contentDescription 화면마다 목록이 달라 문구는 호출부가 정한다.
 */
@Composable
fun GamssScrollToBottomButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ButtonSize)
            .gamssShadow(shape = CircleShape)
            .clip(CircleShape)
            .background(GamssTheme.colors.gray700, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GamssIcons.ScrollDown),
            contentDescription = contentDescription,
            modifier = Modifier.size(IconSize),
            tint = GamssTheme.colors.gray025,
        )
    }
}

private val ButtonSize = 36.dp
private val IconSize = 24.dp
