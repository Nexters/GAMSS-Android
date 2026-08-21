package com.gamss.android.core.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.ui.R
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender

/**
 * 답장 대상을 목록 단위로 한 번만 색인해 두고 아이템별로 꺼내 쓴다. 말풍선마다 목록 전체를 뒤지면
 * 메시지가 하나 늘 때 이미 떠 있던 말풍선까지 전부 재구성 대상이 된다.
 */
class ReplyQuoteLookup internal constructor(private val messagesById: Map<Long, Message>) {

    @Composable
    fun quoteFor(message: Message): ChatReplyQuote? =
        message.repliesToMessageId?.let(messagesById::get)?.toReplyQuote()
}

/** `LazyListScope` 빌더 본문은 @Composable 이 아니라서 색인은 리스트 밖에서 잡아 둬야 한다. */
@Composable
fun rememberReplyQuoteLookup(messages: List<Message>): ReplyQuoteLookup =
    remember(messages) { ReplyQuoteLookup(messages.associateBy(Message::id)) }

@Composable
private fun Message.toReplyQuote(): ChatReplyQuote {
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
