package com.gamss.android.core.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssChatBubbleDefaults
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender

/**
 * 도메인 메시지 하나를 채팅 말풍선으로 그린다. 채팅방과 보관함 카드가 같은 대화를 보여 주므로,
 * 발신자별 좌우 배치와 아바타 선택은 화면마다 갈라지지 않게 여기서 끝낸다.
 *
 * @param replyQuote 호출부(리스트)가 [rememberReplyQuoteLookup] 으로 미리 찾아 넘긴다. 이 버블은
 *  메시지 목록 전체를 몰라도 되고, 목록이 커져도 이 값이 그대로면 재구성을 건너뛴다.
 * @param oppositeWallGap 폭이 좁은 카드 안에서는 [GamssChatBubbleDefaults.CardOppositeWallGap] 을 넘긴다.
 * @param onCharacterMessageClick 넘기지 않으면 말풍선이 눌리지 않는다.
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
                    .noRippleClickableIfNotNull(
                        onCharacterMessageClick?.let { onClick -> { onClick(message) } },
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
