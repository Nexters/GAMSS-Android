package com.gamss.android.core.designsystem.snackbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.theme.GamssTheme

private val SnackBarIconSize = 20.dp

/**
 * 화면 하단에 뜨는 스낵바. 아이콘은 화면마다 다른 것을 붙일 수 있어 nullable 로 둔다.
 * 다른 디자인시스템 컴포넌트들과 마찬가지로 drawable을 painterResource()로 그려 넘기는
 * [Painter]를 받는다 — ImageVector는 material-icons 의존성이 따로 필요해 이 모듈엔 없다.
 * 시안 문구는 한 줄 사용을 권장하지만, 길어지면 그대로 줄바꿈된다.
 */
@Composable
fun GamssSnackBar(
    message: String,
    modifier: Modifier = Modifier,
    snackBarIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GamssTheme.radius.radius100))
            .background(color = GamssTheme.colors.gray900)
            .padding(
                horizontal = GamssTheme.spacing.spacing300,
                vertical = GamssTheme.spacing.spacing200,
            ),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (snackBarIcon != null) {
            Box(
                modifier = Modifier.size(SnackBarIconSize),
                contentAlignment = Alignment.Center,
            ) {
                snackBarIcon()
            }
        }
        Text(
            text = message,
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray025,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 402)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssSnackBarLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssSnackBarPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 402,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssSnackBarDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssSnackBarPreviewContent()
    }
}

@Composable
private fun GamssSnackBarPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
    ) {
        // 아이콘이 있는 스낵바.
        GamssSnackBar(
            message = "이것은 아이콘이 있는 스낵바입니다.\n스낵바는 한 줄로 사용하기를 권장합니다.",
            snackBarIcon = {
                Icon(
                    painter = painterResource(GamssIcons.InfoFilled),
                    contentDescription = null,
                    tint = GamssTheme.colors.yellow,
                )
            },
        )
        // 아이콘이 없는 스낵바.
        GamssSnackBar(
            message = "이것은 아이콘이 없는 스낵바입니다.",
        )
    }
}
