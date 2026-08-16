package com.gamss.android.feature.chat.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssInputBar
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.feature.chat.R
import com.gamss.android.feature.chat.ReplyTarget

@Composable
fun MessageInputBar(
    input: String,
    enabled: Boolean,
    replyTarget: ReplyTarget?,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onReplyClear: () -> Unit,
) {
    GamssInputBar(
        value = input,
        onValueChange = onInputChange,
        onTrailingClick = onSendClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        placeholder = stringResource(R.string.chat_room_input_placeholder),
        trailingContentDescription = "보내기",
        enabled = enabled,
        maxLines = MESSAGE_INPUT_MAX_LINES,
        replyQuote = replyTarget?.let {
            ChatReplyQuote(
                senderLabel = stringResource(
                    R.string.chat_room_reply_to_character,
                    it.characterName
                ),
                message = it.content,
            )
        },
        onReplyClear = onReplyClear,
        replyClearContentDescription = "답장 취소",
    )
}

private const val MESSAGE_INPUT_MAX_LINES = 5
