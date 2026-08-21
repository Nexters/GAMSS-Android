package com.gamss.android.feature.archive.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.card.GamssChattingCard
import com.gamss.android.core.designsystem.component.GamssScrollToBottomButton
import com.gamss.android.core.designsystem.component.chat.GamssChatBubbleDefaults
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.chat.ChatMessageBubble
import com.gamss.android.core.ui.chat.rememberReplyQuoteLookup
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.ConversationCard
import com.gamss.android.feature.archive.R
import kotlinx.coroutines.launch

/**
 * 감정 카드에서 대화보기로 뒤집은 카드. 종료된 대화라 읽기만 하므로 입력창 없이 기록만 보여 준다.
 */
@Composable
internal fun ConversationCardDialog(
    conversationCard: ConversationCard,
    onDismiss: () -> Unit,
) {
    CardDialogScaffold(onDismiss = onDismiss) {
        GamssChattingCard(
            date = conversationCard.card.date.format(CardDateFormatter),
            topEndAction = { CardCloseButton(onClick = onDismiss) },
        ) {
            ConversationMessages(
                messages = conversationCard.messages,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConversationMessages(
    messages: List<Message>,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.archive_conversation_empty),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
            )
        }
        return
    }

    val replyQuotes = rememberReplyQuoteLookup(messages)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing300),
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    replyQuote = replyQuotes.quoteFor(message),
                    oppositeWallGap = GamssChatBubbleDefaults.CardOppositeWallGap,
                )
            }
        }

        if (listState.canScrollForward) {
            GamssScrollToBottomButton(
                onClick = { scope.launch { listState.scrollToItem(messages.lastIndex) } },
                contentDescription = stringResource(R.string.archive_conversation_scroll_to_bottom),
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ConversationCardDialogLightPreview() {
    GamssTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            ConversationCardPreviewContent()
        }
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
private fun ConversationCardDialogDarkPreview() {
    GamssTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            ConversationCardPreviewContent()
        }
    }
}

@Composable
private fun ConversationCardPreviewContent() {
    GamssChattingCard(
        date = "26.08.03",
        topEndAction = { CardCloseButton(onClick = {}) },
    ) {
        ConversationMessages(
            messages = PreviewMessages,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val PREVIEW_CONVERSATION_ID = 10L

private val PreviewMessages = listOf(
    previewMessage(id = 1L, sender = MessageSender.User, content = "안녕하세요ㅁㅇㄹㅁㅇㄹㅁㅇㄹ", time = "오후 1:37"),
    previewMessage(
        id = 2L,
        sender = MessageSender.Character(EmotionCharacter.JOY),
        content = "안녕! 오늘도 행복한 하루~!",
        time = "오후 1:38",
    ),
    previewMessage(
        id = 3L,
        sender = MessageSender.Character(EmotionCharacter.PRICKLY),
        content = "뭐가안녕한데 ㅋㅋ",
        time = "오후 1:38",
        repliesToMessageId = 1L,
    ),
    previewMessage(
        id = 4L,
        sender = MessageSender.User,
        content = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는",
        time = "오후 1:39",
    ),
)

private fun previewMessage(
    id: Long,
    sender: MessageSender,
    content: String,
    time: String,
    repliesToMessageId: Long? = null,
) = Message(
    id = id,
    conversationId = PREVIEW_CONVERSATION_ID,
    sender = sender,
    content = content,
    repliesToMessageId = repliesToMessageId,
    createdTime = time,
)
