package com.gamss.android.feature.chat.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.modifier.gamssShadow
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter

// 디자인 상 각지게 그려진다(둥근 pill 아님).
private val ToastShape = RectangleShape
private val AvatarSize = 16.dp
private val ToastStrokeWidth = 1.dp
private val ToastHorizontalMargin = 40.dp
private val ToastHorizontalPadding = 16.dp
private val ToastVerticalPadding = 10.dp
private val ToastContentGap = 8.dp

/**
 * 스크롤을 올려 최신 메시지가 화면 밖으로 벗어났을 때, 새로 도착한 메시지를 알리는 토스트.
 *
 * 좌우 [ToastHorizontalMargin]까지만 늘어나고 그 안에서 내용에 맞춰 너비가 줄어든다. 넘치는
 * 내용은 말줄임된다. 탭하기 전까지 자동으로 사라지지 않으므로 dismiss 콜백은 따로 두지 않고,
 * 탭하면 [onClick] 하나로 "확인 + 최신 메시지로 이동"을 함께 처리한다.
 */
@Composable
fun NewMessageToast(
    message: Message,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 발신자는 항상 캐릭터다 — 본인이 보낸 메시지는 호출부에서 애초에 이 토스트 대상으로 넘기지
    // 않는다. Unknown 은 MessageBubble 과 동일하게 빈 이름으로 둔다.
    val senderName = when (val sender = message.sender) {
        is MessageSender.Character -> sender.character.displayName
        MessageSender.User, MessageSender.Unknown -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ToastHorizontalMargin)
            .gamssShadow(shape = ToastShape)
            .clip(ToastShape)
            .background(GamssTheme.colors.gray800, ToastShape)
            .border(ToastStrokeWidth, GamssTheme.colors.gray950, ToastShape)
            .noRippleCombinedClickable(onClick = onClick, onLongClick = null)
            .padding(horizontal = ToastHorizontalPadding, vertical = ToastVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToastAvatar()
        GamssText(
            text = senderName,
            modifier = Modifier.padding(start = ToastContentGap),
            style = GamssTheme.typography.body5Medium,
            color = GamssTheme.colors.gray300,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GamssText(
            text = message.content,
            modifier = Modifier
                .padding(start = ToastContentGap)
                .weight(weight = 1f, fill = false),
            style = GamssTheme.typography.body5Medium,
            color = GamssTheme.colors.gray025,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToastAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(GamssTheme.colors.gray025),
    )
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun NewMessageToastLightPreview() {
    GamssTheme(darkTheme = false) {
        NewMessageToastPreviewContent()
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
private fun NewMessageToastDarkPreview() {
    GamssTheme(darkTheme = true) {
        NewMessageToastPreviewContent()
    }
}

@Composable
private fun NewMessageToastPreviewContent() {
    NewMessageToast(
        message = Message(
            id = 1,
            conversationId = 1,
            sender = MessageSender.Character(EmotionCharacter.PRICKLY),
            content = "안녕하세요",
            createdTime = "오후 1:38",
        ),
        onClick = {},
    )
}
