package com.gamss.android.feature.chat.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssLoadingMessageBubble
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
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
                sender = ChatSender(
                    name = sender.character.displayName,
                    avatar = { CharacterAvatarImage(sender.character) },
                ),
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

/**
 * @param character 다음에 도착할 답장의 발신자. pendingComments의 첫 메시지 등으로 미리 알 때
 *  넘기면 빈 프로필 대신 그 캐릭터 아바타와 이름을 보여준다.
 */
@Composable
fun LoadingMessageBubble(character: EmotionCharacter? = null) {
    val senderStatus = if (character != null) {
        character.displayName
    } else {
        stringResource(R.string.chat_room_message_writing_label)
    }
    GamssLoadingMessageBubble(
        senderStatus = senderStatus,
        avatar = character?.let { { CharacterAvatarImage(it) } },
    )
}

@Composable
private fun CharacterAvatarImage(character: EmotionCharacter) {
    Image(
        painter = painterResource(character.avatarIconRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
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
