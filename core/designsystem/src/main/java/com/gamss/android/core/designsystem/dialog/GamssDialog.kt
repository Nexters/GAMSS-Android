package com.gamss.android.core.designsystem.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * Figma: 각종 포폴 이력서 > Dialog (node-id=561-392)
 *
 * title + 좌우 2버튼으로 구성된 확인용 다이얼로그. 오른쪽 버튼([dismissButtonLabel])은
 * 항상 [GamssButtonVariant.Primary]로, "계속 사용하기"처럼 다이얼로그를 닫는 동작을
 * 담당한다. 왼쪽 버튼([actionButtonLabel])은 실제로 확인해야 할 동작(로그아웃, 탈퇴 등)이라
 * 위험도에 따라 [actionButtonVariant]를 [GamssButtonVariant.Neutral] 또는
 * [GamssButtonVariant.Destructive]로 선택한다.
 */
@Composable
fun GamssConfirmDialog(
    title: String,
    actionButtonLabel: String,
    dismissButtonLabel: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionButtonVariant: GamssButtonVariant = GamssButtonVariant.Neutral,
    onDismissRequest: () -> Unit = onDismissClick,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GamssConfirmDialogContent(
            modifier = modifier,
            title = title,
            actionButtonLabel = actionButtonLabel,
            dismissButtonLabel = dismissButtonLabel,
            onActionClick = onActionClick,
            onDismissClick = onDismissClick,
            actionButtonVariant = actionButtonVariant,
        )
    }
}

@Composable
private fun GamssConfirmDialogContent(
    title: String,
    actionButtonLabel: String,
    dismissButtonLabel: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionButtonVariant: GamssButtonVariant = GamssButtonVariant.Neutral,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GamssTheme.colors.white,
                shape = RoundedCornerShape(GamssTheme.radius.radius300),
            )
            .padding(
                top = GamssTheme.spacing.spacing400,
                start = GamssTheme.spacing.spacing400,
                end = GamssTheme.spacing.spacing400,
                bottom = GamssTheme.spacing.spacing300,
            ),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing400),
    ) {
        Text(
            text = title,
            style = GamssTheme.typography.subtitle2,
            color = GamssTheme.colors.gray950,
        )

        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        ) {
            GamssButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = actionButtonLabel,
                variant = actionButtonVariant,
                onClick = onActionClick,
            )
            GamssButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = dismissButtonLabel,
                variant = GamssButtonVariant.Primary,
                onClick = onDismissClick,
            )
        }
    }
}

@Preview(name = "Logout", showBackground = true)
@Composable
private fun GamssConfirmDialogLogoutPreview() {
    GamssTheme {
        GamssConfirmDialogContent(
            title = "로그아웃 하시겠습니까?",
            actionButtonLabel = "로그아웃",
            dismissButtonLabel = "마저 사용하기",
            onActionClick = {},
            onDismissClick = {},
            actionButtonVariant = GamssButtonVariant.Neutral,
        )
    }
}

@Preview(name = "Withdraw", showBackground = true)
@Composable
private fun GamssConfirmDialogWithdrawPreview() {
    GamssTheme {
        GamssConfirmDialogContent(
            title = "탈퇴 하시겠습니까?",
            actionButtonLabel = "탈퇴하기",
            dismissButtonLabel = "마저 사용하기",
            onActionClick = {},
            onDismissClick = {},
            actionButtonVariant = GamssButtonVariant.Destructive,
        )
    }
}
