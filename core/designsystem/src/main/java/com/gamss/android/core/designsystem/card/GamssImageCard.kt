package com.gamss.android.core.designsystem.card

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
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
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.LightGamssColors
import com.gamss.android.core.designsystem.theme.LocalGamssColors

@Composable
fun GamssImageCard(
    date: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topEndAction: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalGamssColors provides LightGamssColors) {
        Box(
            modifier = modifier
                .widthIn(max = CardWidth)
                .fillMaxWidth()
                .aspectRatio(CardAspectRatio)
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

/**
 * 카드 하단의 버튼 두 개와 공유 행에 필요한 문구·동작 묶음.
 *
 * 여섯 값이 늘 함께 쓰이고 함께 사라지므로 한 덩어리로 받는다. [GamssEmotionCard] 에 `null` 을
 * 넘기면 이 영역을 아예 그리지 않아, 누를 수 없는 버튼이 남는 상태를 만들 수 없다.
 */
data class GamssEmotionCardActions(
    val primaryLabel: String,
    val secondaryLabel: String,
    val shareLabel: String,
    val onPrimaryClick: () -> Unit,
    val onSecondaryClick: () -> Unit,
    val onShareClick: () -> Unit,
)

/**
 * 감정 캐릭터와 대화 요약을 보여 주는 이미지 카드 퍼사드.
 *
 * 카드의 고정 구조와 감정별 캐릭터 선택은 이 컴포넌트가 맡고, 문구와 사용자 동작만 호출부가 제공한다.
 * 따라서 화면마다 [GamssImageCard]의 간격과 텍스트 스타일을 다시 조합할 필요가 없다.
 *
 * [actions] 가 `null` 이면 점선과 그 아래 버튼·공유 행을 그리지 않는다. 카드를 이미지로 내보낼 때
 * 눌릴 수 없는 버튼이 그림에 남지 않게 하려는 용도다. 카드 높이는 [CardAspectRatio] 로 고정이라
 * 그만큼 아래가 빈 채로 남는다.
 */
@Composable
fun GamssEmotionCard(
    date: String,
    character: GamssEmotionCardCharacter,
    title: String,
    description: String,
    actions: GamssEmotionCardActions?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topEndAction: @Composable BoxScope.() -> Unit = {},
) {
    GamssImageCard(
        date = date,
        modifier = modifier,
        shape = shape,
        topEndAction = topEndAction,
    ) {
        GamssEmotionCardContent(character = character, showDivider = true) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.title2,
                color = GamssTheme.colors.gray950,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.body4Regular,
                color = GamssTheme.colors.gray800,
                textAlign = TextAlign.Center,
                // Figma Description 은 높이 60 / lineHeight 20 으로 3줄까지 담는다.
                maxLines = DESCRIPTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (actions != null) {
                Spacer(modifier = Modifier.height(DividerToContentGap))
                GamssCardDashedDivider()
                Spacer(modifier = Modifier.height(DividerToContentGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
                ) {
                    CardOutlinedButton(
                        text = actions.primaryLabel,
                        onClick = actions.onPrimaryClick,
                        modifier = Modifier.weight(1f),
                    )
                    CardOutlinedButton(
                        text = actions.secondaryLabel,
                        onClick = actions.onSecondaryClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickableIfNotNull(actions.onShareClick),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actions.shareLabel,
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
    }
}

/**
 * 감정 카드의 캐릭터 영역과 그 아래 콘텐츠 간격을 재사용한다.
 *
 * [showDivider] 를 켜면 캐릭터와 콘텐츠 사이에 점선을 넣는다. 카드 목록처럼 점선이 없는
 * 시안도 같은 캐릭터 영역을 쓰므로 기본값은 꺼짐이다.
 */
@Composable
fun ColumnScope.GamssEmotionCardContent(
    character: GamssEmotionCardCharacter,
    showDivider: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(modifier = Modifier.height(DateToCharacterGap))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CharacterImageHeight),
    ) {
        GamssEmotionCardCharacterImage(character = character)
    }
    if (showDivider) {
        Spacer(modifier = Modifier.height(CharacterToDividerGap))
        GamssCardDashedDivider()
        Spacer(modifier = Modifier.height(DividerToContentGap))
    } else {
        Spacer(modifier = Modifier.height(CharacterToTitleGap))
    }
    content()
}

/** 카드 안을 가로로 끊어 주는 점선. 콘텐츠 열 전체 폭을 쓴다. */
@Composable
private fun GamssCardDashedDivider(modifier: Modifier = Modifier) {
    val color = GamssTheme.colors.gray950
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CardDividerThickness),
    ) {
        val y = size.height / 2f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(CardDividerDashLength.toPx(), CardDividerDashGap.toPx()),
            ),
        )
    }
}

/**
 * 이미지 카드 안에 대화 로그를 배치하는 카드 퍼사드.
 *
 * 대화 내용 자체는 화면마다 달라 [content] 슬롯으로 남기고, 카드의 상단 여백과 셸만 고정한다.
 */
@Composable
fun GamssChattingCard(
    date: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topEndAction: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    GamssImageCard(
        date = date,
        modifier = modifier,
        shape = shape,
        topEndAction = topEndAction,
    ) {
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing300))
        content()
    }
}

@Composable
private fun EmotionCardPreviewContent() {
    GamssEmotionCard(
        date = "26.08.03",
        character = GamssEmotionCardCharacter.ANGER,
        title = "오늘 화~나네",
        description = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
        actions = GamssEmotionCardActions(
            primaryLabel = "기록 버리기",
            secondaryLabel = "대화보기",
            shareLabel = "공유하기",
            onPrimaryClick = {},
            onSecondaryClick = {},
            onShareClick = {},
        ),
        topEndAction = { CloseIconPlaceholder() },
    )
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
    onClick: () -> Unit,
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

@Composable
private fun ChattingCardPreviewContent() {
    GamssChattingCard(
        date = "26.08.03",
        topEndAction = { CloseIconPlaceholder() },
    ) {
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

private val CardWidth = 366.dp
private val CardHeight = 528.dp
private val CardAspectRatio = CardWidth.value / CardHeight.value

private val DateToCharacterGap = 18.dp
private val CharacterImageHeight = 156.dp
private val CharacterToTitleGap = 42.dp

// 점선 관련 값은 Figma Card(3557:5286)의 Divider 기준이다. 점선은 콘텐츠 열 전체 폭(270)을
// 쓰고 위아래 22 씩 띄우며, 캐릭터 이미지와는 16 만 띄운다.
private val CharacterToDividerGap = 16.dp
private val DividerToContentGap = 22.dp
private val CardDividerThickness = 1.3.dp
private val CardDividerDashLength = 6.dp
private val CardDividerDashGap = 4.dp
private const val DESCRIPTION_MAX_LINES = 3
