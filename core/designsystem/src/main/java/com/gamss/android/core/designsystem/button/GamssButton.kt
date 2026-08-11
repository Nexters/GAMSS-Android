package com.gamss.android.core.designsystem.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

enum class GamssButtonVariant {
    Primary,
    Neutral,
    Destructive,
}

/**
 * Figma: 각종 포폴 이력서 > Dialog (node-id=561-392)의 버튼 스타일을 공용화한 컴포넌트.
 *
 * [enabled] = false인 버튼은 시각적으로도 비활성 톤으로 보이도록 [variant]를
 * [GamssButtonVariant.Neutral]과 함께 넘기는 걸 권장한다 (예: 저장 가능 여부에 따라
 * `variant = if (canSave) Primary else Neutral, enabled = canSave`).
 */
@Composable
fun GamssButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GamssButtonVariant = GamssButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val (backgroundColor, contentColor) = when (variant) {
        GamssButtonVariant.Primary -> GamssTheme.colors.gray950 to GamssTheme.colors.white
        GamssButtonVariant.Neutral -> GamssTheme.colors.gray100 to GamssTheme.colors.gray500
        GamssButtonVariant.Destructive -> GamssTheme.colors.red.copy(alpha = 0.15f) to GamssTheme.colors.red
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GamssTheme.radius.radius200))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = GamssTheme.spacing.spacing300,
                vertical = GamssTheme.spacing.spacing300,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = GamssTheme.typography.title5,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Variants", showBackground = true)
@Composable
private fun GamssButtonVariantsPreview() {
    GamssTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GamssButton(label = "마저 사용하기", onClick = {}, variant = GamssButtonVariant.Primary)
            GamssButton(label = "로그아웃", onClick = {}, variant = GamssButtonVariant.Neutral)
            GamssButton(label = "탈퇴하기", onClick = {}, variant = GamssButtonVariant.Destructive)
            GamssButton(
                label = "저장하기",
                onClick = {},
                variant = GamssButtonVariant.Neutral,
                enabled = false,
            )
        }
    }
}
