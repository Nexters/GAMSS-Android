package com.gamss.android.core.designsystem.dialog

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 제목과 선택적인 설명, 한 개 또는 두 개의 액션으로 구성된 GAMSS 공용 다이얼로그입니다.
 *
 * [primaryAction]은 단독으로 사용하면 전체 너비를 차지하고, [secondaryAction]이 있으면
 * 보조 액션은 왼쪽, 주요 액션은 오른쪽에 동일한 너비로 배치됩니다. 액션 실행 후 다이얼로그를
 * 닫을지는 호출 화면이 각 액션의 `onClick`에서 결정합니다.
 */
@Suppress("LongParameterList")
@Composable
fun GamssDialog(
    title: String,
    primaryAction: GamssDialogAction,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryAction: GamssDialogAction? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GamssDialogContent(
            title = title,
            primaryAction = primaryAction,
            modifier = modifier,
            subtitle = subtitle,
            secondaryAction = secondaryAction,
        )
    }
}

@Composable
private fun GamssDialogContent(
    title: String,
    primaryAction: GamssDialogAction,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryAction: GamssDialogAction? = null,
) {
    val visibleSubtitle = subtitle?.takeIf(String::isNotBlank)
    val contentSpacing = if (visibleSubtitle == null) {
        GamssTheme.spacing.spacing600
    } else {
        GamssTheme.spacing.spacing400
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GamssTheme.colors.gray025,
                shape = RoundedCornerShape(GamssTheme.radius.radius300),
            )
            .padding(
                start = GamssTheme.spacing.spacing400,
                top = contentSpacing,
                end = GamssTheme.spacing.spacing400,
                bottom = GamssTheme.spacing.spacing400,
            ),
    ) {
        Text(
            text = title,
            style = GamssTheme.typography.subtitle2,
            color = GamssTheme.colors.gray950,
        )

        if (visibleSubtitle != null) {
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing100))
            Text(
                text = visibleSubtitle,
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
            )
        }

        Spacer(modifier = Modifier.height(contentSpacing))

        DialogActions(
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
        )
    }
}

@Composable
private fun DialogActions(
    primaryAction: GamssDialogAction,
    secondaryAction: GamssDialogAction?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
    ) {
        if (secondaryAction != null) {
            GamssDialogButton(
                modifier = Modifier.weight(1f),
                action = secondaryAction,
            )
        }
        GamssDialogButton(
            modifier = Modifier.weight(1f),
            action = primaryAction,
        )
    }
}

@Composable
private fun GamssDialogButton(
    action: GamssDialogAction,
    modifier: Modifier = Modifier,
) {
    GamssButton(
        modifier = modifier,
        label = action.label,
        onClick = action.onClick,
        variant = action.variant,
        enabled = action.enabled,
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssDialogLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssDialogPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssDialogDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssDialogPreviewContent()
    }
}

@Composable
private fun GamssDialogPreviewContent() {
    val primaryAction = GamssDialogAction(label = "Text", onClick = {})
    val secondaryAction = GamssDialogAction(
        label = "Text",
        onClick = {},
        variant = GamssButtonVariant.Secondary,
    )

    Column(
        modifier = Modifier
            .background(GamssTheme.colors.gray050)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GamssDialogContent(
            title = "Title",
            subtitle = "Subtitle",
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
        )
        GamssDialogContent(
            title = "Title",
            subtitle = "Subtitle",
            primaryAction = primaryAction,
        )
        GamssDialogContent(
            title = "Title",
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
        )
        GamssDialogContent(
            title = "Title",
            primaryAction = primaryAction,
        )
    }
}
