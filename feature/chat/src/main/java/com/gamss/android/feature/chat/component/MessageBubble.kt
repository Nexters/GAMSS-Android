package com.gamss.android.feature.chat.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssLoadingMessageBubble
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.feature.chat.R

/**
 * @param messages 답장 대상 메시지의 내용을 찾기 위한 전체 목록. [Message.repliesToMessageId] 가
 *  가리키는 메시지가 이 목록에 없으면(예: 아직 로드되지 않은 과거 메시지) 답장 인용 없이 표시한다.
 */
@Composable
fun MessageBubble(
    message: Message,
    messages: List<Message>,
    onCharacterMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val replyQuote = message.repliesToMessageId
        ?.let { targetId -> messages.find { it.id == targetId } }
        ?.toReplyQuote()

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        when (val sender = message.sender) {
            MessageSender.User -> GamssSentChatBubble(
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            is MessageSender.Character -> GamssReceivedChatBubble(
                sender = ChatSender(name = sender.character.displayName),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .noRippleCombinedClickable(
                        onClick = { onCharacterMessageClick(message) },
                        onLongClick = null
                    ),
            )

            MessageSender.Unknown -> GamssReceivedChatBubble(
                sender = ChatSender(name = ""),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

@Composable
fun LoadingMessageBubble() {
    GamssLoadingMessageBubble(senderStatus = stringResource(R.string.chat_room_message_writing_label))
}

@Composable
private fun Message.toReplyQuote(): ChatReplyQuote {
    val label = when (val target = sender) {
        is MessageSender.Character -> stringResource(
            R.string.chat_room_reply_to_character,
            target.character.displayName,
        )

        MessageSender.User -> stringResource(R.string.chat_room_reply_to_me)
        MessageSender.Unknown -> stringResource(R.string.chat_room_reply)
    }
    return ChatReplyQuote(senderLabel = label, message = content)
}
