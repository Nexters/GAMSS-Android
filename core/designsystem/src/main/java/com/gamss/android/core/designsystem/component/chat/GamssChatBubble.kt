package com.gamss.android.core.designsystem.component.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * [GamssSentChatBubble], [GamssReceivedChatBubble] 말풍선 안에 표시할 답장 인용 정보.
 *
 * @param senderLabel "OOO에게 답장" 형태로 이미 조합된 라벨 문구. 문구 조합은 호출부의 책임이다.
 * @param message 인용되는 원본 메시지 내용.
 */
@Immutable
data class ChatReplyQuote(
    val senderLabel: String,
    val message: String,
)

/**
 * [GamssReceivedChatBubble] 발신자 정보.
 *
 * @param avatar 아바타 자리에 그릴 컴포저블. 전달하지 않으면 빈 원형 플레이스홀더가 표시된다.
 */
@Immutable
data class ChatSender(
    val name: String,
    val avatar: (@Composable () -> Unit)? = null,
)

object GamssChatBubbleDefaults {
    /**
     * 말풍선이 최대로 늘어났을 때 반대쪽 벽까지 남겨야 하는 여백.
     * 고정 너비 대신 이 여백을 기준으로 말풍선의 최대 너비를 역산한다.
     */
    val OppositeWallGap: Dp = 84.dp
}

private val AvatarSize = 24.dp

/**
 * 내가 보낸 채팅 메시지 말풍선. 시간이 왼쪽, 말풍선이 오른쪽에 정렬된다.
 */
@Composable
fun GamssSentChatBubble(
    message: String,
    time: String?,
    modifier: Modifier = Modifier,
    replyQuote: ChatReplyQuote? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val bubbleMaxWidth = (maxWidth - GamssChatBubbleDefaults.OppositeWallGap).coerceAtLeast(0.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (time != null) {
                ChatTimeText(time = time)
            }
            ChatBubbleSurface(
                isMine = true,
                modifier = Modifier.widthIn(max = bubbleMaxWidth),
            ) {
                ChatBubbleContent(message = message, replyQuote = replyQuote, isMine = true)
            }
        }
    }
}

/**
 * 상대가 보낸 채팅 메시지 말풍선. 아바타 + 이름 + 말풍선이 왼쪽, 시간이 오른쪽에 정렬된다.
 */
@Composable
fun GamssReceivedChatBubble(
    sender: ChatSender,
    message: String,
    time: String?,
    modifier: Modifier = Modifier,
    replyQuote: ChatReplyQuote? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        // 아바타와 그 옆 간격은 말풍선보다 먼저 고정폭을 차지하므로, 반대쪽 벽 여백을 뺀
        // 나머지 몫에서 그만큼을 한 번 더 제해야 말풍선 자체의 최대 너비가 나온다.
        val reservedWidth = GamssChatBubbleDefaults.OppositeWallGap + AvatarSize + GamssTheme.spacing.spacing100
        val bubbleMaxWidth = (maxWidth - reservedWidth).coerceAtLeast(0.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
                verticalAlignment = Alignment.Top,
            ) {
                ChatAvatar(avatar = sender.avatar)
                Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing075)) {
                    Text(
                        text = sender.name,
                        style = GamssTheme.typography.body4Medium,
                        color = GamssTheme.colors.gray800,
                    )
                    ChatBubbleSurface(
                        isMine = false,
                        modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    ) {
                        ChatBubbleContent(message = message, replyQuote = replyQuote, isMine = false)
                    }
                }
            }
            if (time != null) {
                ChatTimeText(time = time)
            }
        }
    }
}

@Composable
private fun ChatBubbleSurface(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        // 답장 인용문의 구분선(HorizontalDivider)이 본문과 같은 너비로 맞춰지도록
        // 가장 넓은 자식의 고유 너비에 맞춰 hug 하되, widthIn(max) 로 넘어오는 상한은 그대로 유지한다.
        modifier = modifier
            .width(IntrinsicSize.Max)
            .background(if (isMine) GamssTheme.colors.gray900 else GamssTheme.colors.gray025)
            .border(width = 1.dp, color = GamssTheme.colors.gray950)
            .padding(
                horizontal = GamssTheme.spacing.spacing200,
                vertical = GamssTheme.spacing.spacing100
            ),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing075),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        content()
    }
}

@Composable
private fun ChatBubbleContent(
    message: String,
    replyQuote: ChatReplyQuote?,
    isMine: Boolean,
) {
    if (replyQuote != null) {
        Text(
            text = replyQuote.senderLabel,
            style = GamssTheme.typography.body5Medium,
            color = if (isMine) GamssTheme.colors.gray025 else GamssTheme.colors.gray950,
        )
        Text(
            text = replyQuote.message,
            style = GamssTheme.typography.body4Medium,
            color = if (isMine) GamssTheme.colors.gray400 else GamssTheme.colors.gray500,
        )
        HorizontalDivider(color = if (isMine) GamssTheme.colors.gray700 else GamssTheme.colors.gray200)
    }
    Text(
        text = message,
        style = GamssTheme.typography.body4Medium,
        color = if (isMine) GamssTheme.colors.gray025 else GamssTheme.colors.gray950,
    )
}

@Composable
private fun ChatTimeText(time: String, modifier: Modifier = Modifier) {
    Text(
        text = time,
        style = GamssTheme.typography.body6Regular,
        color = GamssTheme.colors.gray600,
        modifier = modifier,
    )
}

@Composable
private fun ChatAvatar(
    avatar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .then(
                if (avatar == null) {
                    Modifier
                        .background(GamssTheme.colors.gray025)
                        .border(
                            width = 1.dp,
                            color = GamssTheme.colors.gray200,
                            shape = CircleShape
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        avatar?.invoke()
    }
}

@Preview(name = "ChatBubble - Sent - Light", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssSentChatBubblePreview() {
    GamssTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .background(GamssTheme.colors.gray025)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            GamssSentChatBubble(message = "안녕하세요ㅁㅇㄹㅁㅇㄹㅁㅇㄹ", time = "오후 1:37")
            GamssSentChatBubble(
                message = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이 응궁로겨 춀잔",
                time = "오후 1:38",
            )
            GamssSentChatBubble(
                message = "설느닛람햄을 긱에자네에 신손 겅투",
                time = "오후 1:39",
                replyQuote = ChatReplyQuote(senderLabel = "불안이에게 답장", message = "안녕하세용"),
            )
        }
    }
}

@Preview(name = "ChatBubble - Received - Light", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssReceivedChatBubblePreview() {
    GamssTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .background(GamssTheme.colors.gray025)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GamssReceivedChatBubble(
                sender = ChatSender(name = "기쁨이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
            )
            GamssReceivedChatBubble(
                sender = ChatSender(name = "까칠이"),
                message = "뭐가안녕한데 ㅋㅋ",
                time = "오후 1:38",
                replyQuote = ChatReplyQuote(senderLabel = "불안이에게 답장", message = "안녕하세용"),
            )
        }
    }
}

@Preview(name = "ChatBubble - Conversation - Dark", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssChatBubbleConversationDarkPreview() {
    GamssTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(GamssTheme.colors.gray025)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GamssSentChatBubble(message = "안녕하세요!", time = "오후 1:37", modifier = Modifier.align(Alignment.End))
            GamssReceivedChatBubble(
                sender = ChatSender(name = "기쁨이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
            )
        }
    }
}
