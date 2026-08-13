package com.gamss.android.feature.chat.component

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.dialog.GamssDialog
import com.gamss.android.core.designsystem.dialog.GamssDialogAction
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.chat.R

/**
 * 대화 삭제 확인. 되돌릴 수 없는 동작이라 실행 전에 한 번 끊습니다.
 *
 * 물렀을 때 고른 방을 비우지 않는 것은 호출 화면의 몫입니다. 이 컴포저블은 문구와 버튼 배치만 맡습니다.
 */
@Composable
internal fun DeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.chatting_list_delete_dialog_title),
        subtitle = stringResource(R.string.chatting_list_delete_dialog_subtitle),
        primaryAction = GamssDialogAction(
            label = stringResource(R.string.chatting_list_delete_dialog_confirm),
            onClick = onConfirm,
            variant = GamssButtonVariant.Destructive,
        ),
        secondaryAction = GamssDialogAction(
            label = stringResource(R.string.chatting_list_delete_dialog_dismiss),
            onClick = onDismiss,
            variant = GamssButtonVariant.Secondary,
        ),
        onDismissRequest = onDismiss,
    )
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun DeleteConversationDialogLightPreview() {
    GamssTheme(darkTheme = false) {
        DeleteConversationDialogPreviewContent()
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
private fun DeleteConversationDialogDarkPreview() {
    GamssTheme(darkTheme = true) {
        DeleteConversationDialogPreviewContent()
    }
}

@Composable
private fun DeleteConversationDialogPreviewContent() {
    DeleteConversationDialog(onConfirm = {}, onDismiss = {})
}
