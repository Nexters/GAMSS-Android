package com.gamss.android.core.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssChatBubbleDefaults
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.ui.R
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender

/**
 * 도메인 메시지 하나를 채팅 말풍선으로 그린다.
 *
 * 발신자에 따른 좌우 배치와 아바타 선택까지 여기서 끝낸다. 채팅방과 보관함 카드처럼 같은 대화를
 * 보여 주는 화면이 여럿이라, 이 매핑이 화면마다 갈라지면 한쪽만 시안과 어긋나게 된다.
 *
 * @param replyQuote [message]가 답장이면 그 대상의 인용 정보. 호출부(리스트)가 한 번에 미리
 *  찾아서 넘긴다 — 이 버블은 전체 메시지 목록을 몰라도 되고, 목록이 커져도(다른 메시지가
 *  추가돼도) 이 값이 그대로면 재구성을 건너뛸 수 있다.
 * @param oppositeWallGap 말풍선 최대 너비를 정하는 반대쪽 벽 여백. 폭이 좁은 카드 안에서는
 *  [GamssChatBubbleDefaults.CardOppositeWallGap] 처럼 더 작은 값을 넘긴다.
 * @param onCharacterMessageClick 캐릭터 말풍선을 눌렀을 때 동작. 넘기지 않으면 누를 수 없다.
 */
@Composable
fun ChatMessageBubble(
    message: Message,
    replyQuote: ChatReplyQuote?,
    modifier: Modifier = Modifier,
    oppositeWallGap: Dp = GamssChatBubbleDefaults.OppositeWallGap,
    onCharacterMessageClick: ((Message) -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        when (val sender = message.sender) {
            MessageSender.User -> GamssSentChatBubble(
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                oppositeWallGap = oppositeWallGap,
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            is MessageSender.Character -> GamssReceivedChatBubble(
                sender = ChatSender(
                    name = sender.character.displayName,
                    avatar = { CharacterAvatar(sender.character) },
                ),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                oppositeWallGap = oppositeWallGap,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .noRippleCombinedClickable(
                        onClick = onCharacterMessageClick?.let { onClick -> { onClick(message) } },
                        onLongClick = null,
                    ),
            )

            MessageSender.Unknown -> GamssReceivedChatBubble(
                sender = ChatSender(name = ""),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                oppositeWallGap = oppositeWallGap,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/** 답장 대상 메시지를 말풍선 안에 넣을 인용문으로 옮긴다. */
@Composable
fun Message.toReplyQuote(): ChatReplyQuote {
    val label = when (val target = sender) {
        is MessageSender.Character -> stringResource(
            R.string.chat_reply_to_character,
            target.character.displayName,
        )

        MessageSender.User -> stringResource(R.string.chat_reply_to_me)
        MessageSender.Unknown -> stringResource(R.string.chat_reply)
    }
    return ChatReplyQuote(senderLabel = label, message = content)
}
