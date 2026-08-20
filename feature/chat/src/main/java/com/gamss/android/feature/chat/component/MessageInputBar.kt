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
    isSending: Boolean,
    isTokenExhausted: Boolean,
    replyTarget: ReplyTarget?,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onReplyClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GamssInputBar(
        value = input,
        onValueChange = onInputChange,
        onTrailingClick = onSendClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        placeholder = if (isTokenExhausted) {
            stringResource(R.string.chat_room_input_placeholder_exhausted)
        } else {
            stringResource(R.string.chat_room_input_placeholder)
        },
        trailingContentDescription = "보내기",
        // 토큰이 소진되면 다시 채울 방법이 없으니(다음 날까지) 입력칸 자체를 잠가 자리표시자
        // 문구로 안내한다. 전송 중일 때는 입력은 유지하고 전송 버튼만 잠근다.
        enabled = enabled && !isTokenExhausted,
        sendEnabled = enabled && !isSending && !isTokenExhausted,
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
