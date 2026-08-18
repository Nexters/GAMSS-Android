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
 * @param replyQuote [message]가 답장이면 그 대상의 인용 정보. 호출부(리스트)가 한 번에 미리
 *  찾아서 넘긴다 — 이 버블은 전체 메시지 목록을 몰라도 되고, 목록이 커져도(다른 메시지가
 *  추가돼도) 이 값이 그대로면 재구성을 건너뛸 수 있다.
 */
@Composable
fun MessageBubble(
    message: Message,
    replyQuote: ChatReplyQuote?,
    onCharacterMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
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

/** [ChatRoomScreen]의 리스트가 아이템별 [MessageBubble.replyQuote]를 미리 계산할 때도 쓴다. */
@Composable
internal fun Message.toReplyQuote(): ChatReplyQuote {
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
