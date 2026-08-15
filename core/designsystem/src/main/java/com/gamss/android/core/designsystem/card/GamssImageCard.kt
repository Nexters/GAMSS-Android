package com.gamss.android.core.designsystem.card

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 캐릭터 배경 이미지 위에 콘텐츠를 얹는 카드 셸. 배경은 모든 variant가 공유하는 고정 에셋이라
 * 파라미터로 받지 않는다.
 *
 * 실측이 고정인 건 카드 크기(366x528), 날짜 위치, 우상단 액션 아이콘 위치뿐이다. 그 아래 본문은
 * variant마다(버튼형 요약 카드, 채팅 로그 카드 ...) 구조가 완전히 달라 셸이 알 필요가 없어
 * [content] 슬롯으로 그대로 넘긴다. [topEndAction] 도 슬롯인 이유는 같다 — 아이콘 에셋이 아직
 * 없어 지금은 호출부가 플레이스홀더를 그리고, 나중에 실제 에셋으로 바꿔도 셸은 손댈 일이 없다.
 */
@Composable
fun GamssImageCard(
    date: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topEndAction: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .width(CardWidth)
            .height(CardHeight)
            .clip(shape),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_card),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = GamssTheme.spacing.spacing800,
                    end = GamssTheme.spacing.spacing800,
                    top = GamssTheme.spacing.spacing700,
                ),
        ) {
            Text(
                text = date,
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.subtitle3,
                color = GamssTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
            content()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = GamssTheme.spacing.spacing550, end = GamssTheme.spacing.spacing550),
        ) {
            topEndAction()
        }
    }
}

@Preview(name = "Emotion - Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssImageCardEmotionLightPreview() {
    GamssTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            EmotionCardPreviewContent()
        }
    }
}

@Preview(
    name = "Emotion - Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssImageCardEmotionDarkPreview() {
    GamssTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            EmotionCardPreviewContent()
        }
    }
}

/** Figma `Type=Card` 재현. 캐릭터 이미지 + 요약 제목/설명 + 기록 버리기/대화보기 + 공유하기. */
@Composable
private fun EmotionCardPreviewContent() {
    GamssImageCard(
        date = "26.08.03",
        topEndAction = { CloseIconPlaceholder() },
    ) {
        Spacer(modifier = Modifier.height(DateToCharacterGap))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CharacterImageHeight)
                .background(CharacterPlaceholderColor),
        ) {
            Image(
                painter = painterResource(R.drawable.character_angry),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(CharacterToTitleGap))
        Text(
            text = "오늘 화~나네",
            modifier = Modifier.fillMaxWidth(),
            style = GamssTheme.typography.title2,
            color = GamssTheme.colors.gray950,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        Text(
            text = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
            modifier = Modifier.fillMaxWidth(),
            style = GamssTheme.typography.body4Regular,
            color = GamssTheme.colors.gray800,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing800))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
        ) {
            CardOutlinedButton(text = "기록 버리기", modifier = Modifier.weight(1f))
            CardOutlinedButton(text = "대화보기", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "공유하기",
                style = GamssTheme.typography.body5Medium,
                color = GamssTheme.colors.gray600,
            )
            Spacer(modifier = Modifier.width(GamssTheme.spacing.spacing025))
            Icon(
                painter = painterResource(GamssIcons.RightChevron),
                contentDescription = null,
                tint = GamssTheme.colors.gray600,
                modifier = Modifier.size(GamssTheme.spacing.spacing300),
            )
        }
    }
}

/** Figma의 우상단 20dp 닫기 아이콘과 같은 크기·색의 프리뷰 액션. */
@Composable
private fun CloseIconPlaceholder(modifier: Modifier = Modifier) {
    Text(
        text = "×",
        modifier = modifier.size(GamssTheme.spacing.spacing400),
        color = GamssTheme.colors.gray300,
        style = GamssTheme.typography.title3.copy(lineHeight = 20.sp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CardOutlinedButton(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GamssOutlinedCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = GamssTheme.colors.gray900,
        contentPadding = PaddingValues(vertical = GamssTheme.spacing.spacing200),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = GamssTheme.typography.subtitle4,
            color = GamssTheme.colors.gray900,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Chatting - Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssImageCardChattingLightPreview() {
    GamssTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            ChattingCardPreviewContent()
        }
    }
}

@Preview(
    name = "Chatting - Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssImageCardChattingDarkPreview() {
    GamssTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            ChattingCardPreviewContent()
        }
    }
}

/** Figma `Type=Card_chatting` 재현. 같은 셸에 본문만 대화 로그로 바뀐다. */
@Composable
private fun ChattingCardPreviewContent() {
    GamssImageCard(
        date = "26.08.03",
        topEndAction = { CloseIconPlaceholder() },
    ) {
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing300))
        Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing150)) {
            GamssSentChatBubble(
                message = "안녕하세요ㅁㅇㄹㅁㅇㄹㅁㅇㄹ",
                time = "오후 1:37",
                modifier = Modifier.align(Alignment.End),
            )
            GamssReceivedChatBubble(
                sender = ChatSender(name = "기쁨이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
            )
            GamssReceivedChatBubble(
                sender = ChatSender(name = "슬픔이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
            )
        }
    }
}

// 디자인 실측: 카드 366x528. 안쪽 여백(좌우 48/상단 40)과 우상단 액션 아이콘 위치(28,28)는
// GamssSpacing 값과 정확히 맞아떨어져 spacing800/700/550 토큰을 그대로 쓴다.
private val CardWidth = 366.dp
private val CardHeight = 528.dp

private val DateToCharacterGap = 18.dp
private val CharacterImageHeight = 156.dp
private val CharacterToTitleGap = 42.dp

// Figma char_image: #FFA8A8, opacity 30%.
private val CharacterPlaceholderColor = Color(0x4DFFA8A8)
