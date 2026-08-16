package com.gamss.android.feature.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.dialog.GamssDialog
import com.gamss.android.core.designsystem.dialog.GamssDialogAction
import com.gamss.android.feature.chat.R

@Composable
fun EndConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.chat_room_end_dialog_title),
        subtitle = stringResource(R.string.chat_room_end_dialog_subTitle),
        secondaryAction = GamssDialogAction(
            label = stringResource(R.string.chat_room_end_dialog_dismiss_button_label),
            onClick = onDismiss,
            variant = GamssButtonVariant.Secondary,
        ),
        primaryAction = GamssDialogAction(
            label = stringResource(R.string.chat_room_end_dialog_confirm_button_label),
            onClick = onConfirm,
        ),
        onDismissRequest = onDismiss,
    )
}