package com.gamss.android.core.designsystem.component.chat

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme
import kotlin.math.roundToInt

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

    /**
     * 카드 안 대화([com.gamss.android.core.designsystem.card.GamssChattingCard])의 벽 여백.
     * 대화 영역이 270 뿐이라 채팅방과 같은 여백을 남기면 말풍선이 지나치게 좁아진다.
     * Figma Card_Chatting 가이드(3264:7548)의 "maximum: chat 프레임 기준 67px" 기준이다.
     */
    val CardOppositeWallGap: Dp = 67.dp
}

private val AvatarSize = 26.dp
private val AvatarStrokeWidth = 0.5.dp

// 로딩 bubble(54x36) 안에 정확히 맞도록 두 값 다 고정 크기로 지정한다. typing_loading.json
private val TypingLottieWidth = 46.dp
private val TypingLottieHeight = 32.dp

// 46x32 Lottie 를 54x36 bubble 안에 정확히 담기 위한 여백. 실제 메시지 bubble 의
// spacing200/spacing100 패딩과는 다른, 로딩 bubble 전용 값이다.
private val LoadingBubbleHorizontalPadding = 4.dp
private val LoadingBubbleVerticalPadding = 2.dp

/**
 * 내가 보낸 채팅 메시지 말풍선. 시간이 왼쪽, 말풍선이 오른쪽에 정렬된다.
 *
 * @param oppositeWallGap 말풍선이 최대로 늘어났을 때 왼쪽 벽까지 남길 여백.
 */
@Composable
fun GamssSentChatBubble(
    message: String,
    time: String?,
    modifier: Modifier = Modifier,
    replyQuote: ChatReplyQuote? = null,
    oppositeWallGap: Dp = GamssChatBubbleDefaults.OppositeWallGap,
) {
    BoxWithConstraints(modifier = modifier) {
        val bubbleMaxWidth = (maxWidth - oppositeWallGap).coerceAtLeast(0.dp)
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
 *
 * @param oppositeWallGap 말풍선이 최대로 늘어났을 때 오른쪽 벽까지 남길 여백.
 */
@Composable
fun GamssReceivedChatBubble(
    sender: ChatSender,
    message: String,
    time: String?,
    modifier: Modifier = Modifier,
    replyQuote: ChatReplyQuote? = null,
    oppositeWallGap: Dp = GamssChatBubbleDefaults.OppositeWallGap,
) {
    BoxWithConstraints(modifier = modifier) {
        val reservedWidth = oppositeWallGap + AvatarSize + GamssTheme.spacing.spacing100
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

/**
 * 상대가 입력중일때 말풍선. 아바타 + 이름 + 말풍선이 왼쪽에 정렬된다.
 *
 * @param avatar 다음에 도착할 답장의 발신자를 이미 알 때(예: pendingComments) 그 캐릭터 아바타를
 *  넘기면 빈 프로필 대신 표시한다.
 */
@Composable
fun GamssLoadingMessageBubble(
    senderStatus: String,
    avatar: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
            verticalAlignment = Alignment.Top,
        ) {
            ChatAvatar(avatar = avatar)
            Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing075)) {
                Text(
                    text = senderStatus,
                    style = GamssTheme.typography.body4Medium,
                    color = GamssTheme.colors.gray800,
                )
                ChatBubbleSurface(
                    isMine = false,
                    horizontalPadding = LoadingBubbleHorizontalPadding,
                    verticalPadding = LoadingBubbleVerticalPadding,
                ) {
                    TypingLottie()
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleSurface(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = GamssTheme.spacing.spacing200,
    verticalPadding: Dp = GamssTheme.spacing.spacing100,
    content: @Composable () -> Unit,
) {
    // painterResource() 는 9-patch를 지원하지 않아
    // rememberNinePatchPainter 로 직접 그린다. fill(배경)·stroke(테두리)는 같은 손그림 윤곽에서
    // 나온 한 쌍이라 어떤 크기로 늘어나도 모서리가 서로 어긋나지 않는다.
    val bubbleColor = if (isMine) GamssTheme.colors.gray900 else GamssTheme.colors.gray025
    Box(modifier = modifier.width(IntrinsicSize.Max)) {
        Image(
            painter = rememberNinePatchPainter(id = R.drawable.chatmessage_fill, tint = bubbleColor),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Image(
            painter = rememberNinePatchPainter(id = R.drawable.chatmessage_stroke, tint = GamssTheme.colors.gray950),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            // 답장 인용문의 구분선(HorizontalDivider)이 본문과 같은 너비로 맞춰지도록
            // 가장 넓은 자식의 고유 너비에 맞춰 hug 하되, widthIn(max) 로 넘어오는 상한은 그대로 유지한다.
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing075),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        ) {
            content()
        }
    }
}

/**
 * 9-patch(.9.png) 전용 [Painter]. `painterResource()` 는 NinePatchDrawable을 BitmapDrawable로
 * 강제 캐스팅해 크래시가 나므로, [Drawable]을 직접 canvas에 그려서 우회한다.
 *
 * @param tint 지정하면 SRC_IN 방식으로 색만 덧칠한다(투명 영역·모양은 그대로 유지).
 */
@Composable
private fun rememberNinePatchPainter(@DrawableRes id: Int, tint: Color? = null): Painter {
    val context = LocalContext.current
    return remember(context, id, tint) {
        val drawable = requireNotNull(ContextCompat.getDrawable(context, id)) {
            "리소스를 찾을 수 없습니다: $id"
        }.mutate()
        if (tint != null) {
            drawable.setTint(tint.toArgb())
        }
        NinePatchPainter(drawable)
    }
}

private class NinePatchPainter(private val drawable: Drawable) : Painter() {
    override val intrinsicSize: Size
        get() = if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
            Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        } else {
            Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

/** 말풍선 안에서 무한 반복 재생되는 타이핑 로티. 54x36 로딩 bubble 안에 정확히 맞도록 고정 크기로 그린다. */
@Composable
private fun TypingLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.typing_loading))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(width = TypingLottieWidth, height = TypingLottieHeight),
    )
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
            .background(GamssTheme.colors.gray025)
            .border(width = AvatarStrokeWidth, color = GamssTheme.colors.gray200, shape = CircleShape),
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
