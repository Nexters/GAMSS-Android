package com.gamss.android.core.designsystem.button

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * GAMSS에서 공통으로 사용하는 텍스트 버튼입니다.
 *
 * 버튼 너비는 [modifier]를 통해 화면에 맞게 조절할 수 있으며, 콘텐츠 주변에는 디자인 가이드의
 * 최소 여백 16dp가 항상 적용됩니다. [enabled]가 `false`이면 [variant]와 관계없이 비활성 색상이
 * 적용되고 클릭 이벤트도 전달되지 않습니다.
 *
 * 누를 때 ripple을 그리지 않습니다. 디자인에 눌림 표현이 없어, 기본 indication을 두면 색이
 * 겹쳐 보이는 잔상이 생깁니다.
 */
@Composable
fun GamssButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GamssButtonVariant = GamssButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = gamssButtonColors(variant = variant, enabled = enabled)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GamssTheme.radius.radius200))
            .background(colors.containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = null,
                interactionSource = null,
                onClick = onClick,
            )
            .padding(GamssTheme.spacing.spacing300),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = GamssTheme.typography.title5,
            color = colors.contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun gamssButtonColors(
    variant: GamssButtonVariant,
    enabled: Boolean,
): GamssButtonColors {
    if (!enabled) {
        return GamssButtonColors(
            containerColor = GamssTheme.colors.gray075,
            contentColor = GamssTheme.colors.gray300,
        )
    }

    return when (variant) {
        GamssButtonVariant.Primary -> GamssButtonColors(
            containerColor = GamssTheme.colors.gray950,
            contentColor = GamssTheme.colors.gray025,
        )

        GamssButtonVariant.Secondary -> GamssButtonColors(
            containerColor = GamssTheme.colors.gray100,
            contentColor = GamssTheme.colors.gray600,
        )

        GamssButtonVariant.Destructive -> GamssButtonColors(
            containerColor = GamssTheme.colors.apricot,
            contentColor = GamssTheme.colors.white,
        )
    }
}

private data class GamssButtonColors(
    val containerColor: Color,
    val contentColor: Color,
)

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssButtonLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssButtonPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssButtonDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssButtonPreviewContent()
    }
}

@Composable
private fun GamssButtonPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
    ) {
        GamssButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Enabled",
            onClick = {},
        )
        GamssButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Enabled",
            variant = GamssButtonVariant.Secondary,
            onClick = {},
        )
        GamssButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Disabled",
            enabled = false,
            onClick = {},
        )
        GamssButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Destructive",
            variant = GamssButtonVariant.Destructive,
            onClick = {},
        )
    }
}
