package com.gamss.android.core.designsystem.card

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssChatBubbleDefaults
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.LightGamssColors
import com.gamss.android.core.designsystem.theme.LocalGamssColors

/**
 * 손그림 종이 카드입니다.
 *
 * 종이 비율이 고정이라 늘거나 스크롤할 수 없습니다. 그래서 안쪽 여백과 그림은 카드 배율([cardScale])로
 * 함께 줄이고, 폰트 확대는 [cardFontScale] 상한까지만 반영합니다.
 *
 * @param capFontScale 내용이 카드 안에서 스크롤되는 카드는 넘칠 일이 없어 상한을 끕니다.
 */
@Composable
fun GamssImageCard(
    date: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    capFontScale: Boolean = true,
    topEndAction: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.(scale: Float) -> Unit,
) {
    CompositionLocalProvider(LocalGamssColors provides LightGamssColors) {
        BoxWithConstraints(
            modifier = modifier
                .widthIn(max = GamssCardDefaults.Width)
                .fillMaxWidth(),
        ) {
            val scale = cardScale(maxWidth)
            val density = LocalDensity.current
            val fontScale =
                if (capFontScale) cardFontScale(density.fontScale, scale) else density.fontScale
            // 상한이 걸리지 않으면 원본을 그대로 넘깁니다. 새 Density 는 폰트 변환 캐시를 잃습니다.
            val cardDensity = remember(density, fontScale) {
                if (fontScale == density.fontScale) density else Density(density.density, fontScale)
            }
            CompositionLocalProvider(LocalDensity provides cardDensity) {
                Box(
                    modifier = Modifier
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
                                start = GamssTheme.spacing.spacing800 * scale,
                                end = GamssTheme.spacing.spacing800 * scale,
                                top = GamssTheme.spacing.spacing700 * scale,
                                bottom = CardBorderInset * scale,
                            ),
                    ) {
                        Text(
                            text = date,
                            modifier = Modifier.fillMaxWidth(),
                            style = GamssTheme.typography.subtitle3,
                            color = GamssTheme.colors.gray900,
                            textAlign = TextAlign.Center,
                        )
                        content(scale)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = GamssTheme.spacing.spacing550 * scale,
                                end = GamssTheme.spacing.spacing550 * scale,
                            ),
                    ) {
                        topEndAction()
                    }
                }
            }
        }
    }
}

/** 채팅의 접기 연출이 펼친 종이 칸을 이 크기로 잡습니다. 어긋나면 종이 비율이 깨집니다. */
object GamssCardDefaults {
    val Width: Dp = 366.dp
    val Height: Dp = 528.dp

    /** Figma Description 은 높이 60 / lineHeight 20 으로 3줄까지 담습니다. */
    const val DescriptionMaxLines = 3
}

/** 기준이 화면 폭인 `designScale` 과 다릅니다. 카드는 화면보다 좁은 자리에 놓입니다. */
private fun cardScale(cardWidth: Dp): Float =
    (cardWidth / GamssCardDefaults.Width).coerceAtMost(1f)

/**
 * 상한이 카드 배율을 함께 타는 이유는 카드가 작아진 만큼 글자가 커질 폭도 좁아지기 때문입니다.
 * 상한은 1 아래로 내려가지 않습니다. 그 아래는 상한이 아니라 축소입니다.
 */
private fun cardFontScale(systemFontScale: Float, scale: Float): Float =
    systemFontScale.coerceAtMost((MAX_FONT_SCALE_AT_DESIGN_WIDTH * scale).coerceAtLeast(1f))

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

/** 좁은 폭과 폰트 확대가 각각 세로 예산을 깎으므로 둘 다 둡니다. */
@Preview(name = "Emotion - 360dp", showBackground = true, widthDp = 360, heightDp = 520)
@Preview(name = "Emotion - 320dp", showBackground = true, widthDp = 320, heightDp = 470)
@Preview(
    name = "Emotion - 폰트 2.0",
    showBackground = true,
    widthDp = 412,
    heightDp = 580,
    fontScale = 2f,
)
@Preview(
    name = "Emotion - 360dp + 폰트 2.0",
    showBackground = true,
    widthDp = 360,
    heightDp = 520,
    fontScale = 2f,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssImageCardEmotionNarrowPreview() {
    GamssTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(GamssTheme.spacing.spacing300)) {
            EmotionCardPreviewContent(
                description = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이 " +
                    "머츠니가 도랴흐로 뎌슨하다",
            )
        }
    }
}

/**
 * 카드 아래쪽 점선 밑에 들어가는 것. 시안이 상세 팝업과 공유 이미지 두 가지만 두므로 그대로 나눈다.
 */
sealed interface GamssEmotionCardFooter {

    /**
     * 카드 상세에서 쓰는 버튼 두 개와 공유 행.
     *
     * 여섯 값이 늘 함께 쓰이고 함께 사라지므로 한 덩어리로 받는다.
     */
    data class Actions(
        val primaryLabel: String,
        val secondaryLabel: String,
        val shareLabel: String,
        val onPrimaryClick: () -> Unit,
        val onSecondaryClick: () -> Unit,
        val onShareClick: () -> Unit,
    ) : GamssEmotionCardFooter

    /**
     * 공유 이미지에서 쓰는 GAMSS 워드마크.
     *
     * 그림에는 누를 수 없는 버튼 대신 브랜드를 남긴다. 이 상태를 넘기면 액션 문구와 콜백을 함께
     * 넘길 수 없으므로, 눌리지 않는 버튼이 그려지는 경우를 만들 수 없다.
     */
    data object Brand : GamssEmotionCardFooter
}

/**
 * 감정 캐릭터와 대화 요약을 보여 주는 이미지 카드 퍼사드.
 *
 * 카드의 고정 구조와 감정별 캐릭터 선택은 이 컴포넌트가 맡고, 문구와 사용자 동작만 호출부가 제공한다.
 * 따라서 화면마다 [GamssImageCard]의 간격과 텍스트 스타일을 다시 조합할 필요가 없다.
 *
 * 아래쪽 점선까지는 두 시안이 같고, 그 밑에 무엇이 오는지만 [footer] 가 정한다.
 */
@Composable
fun GamssEmotionCard(
    date: String,
    character: GamssEmotionCardCharacter,
    title: String,
    description: String,
    footer: GamssEmotionCardFooter,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    topEndAction: @Composable BoxScope.() -> Unit = {},
) {
    GamssImageCard(
        date = date,
        modifier = modifier,
        shape = shape,
        topEndAction = topEndAction,
    ) { scale ->
        GamssEmotionCardContent(character = character, scale = scale) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.title2,
                color = GamssTheme.colors.gray950,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200 * scale))
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.body4Regular,
                color = GamssTheme.colors.gray800,
                textAlign = TextAlign.Center,
                maxLines = GamssCardDefaults.DescriptionMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(DividerToContentGap * scale))
            GamssCardDashedDivider()
            Spacer(modifier = Modifier.height(DividerToContentGap * scale))
            when (footer) {
                is GamssEmotionCardFooter.Actions -> CardActionsFooter(footer, scale)
                GamssEmotionCardFooter.Brand -> CardBrandFooter()
            }
        }
    }
}

/** 상세 팝업 아래쪽. 버튼 두 개와 그 밑 공유 행. */
@Composable
private fun CardActionsFooter(actions: GamssEmotionCardFooter.Actions, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100 * scale),
    ) {
        CardOutlinedButton(
            text = actions.primaryLabel,
            onClick = actions.onPrimaryClick,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
        CardOutlinedButton(
            text = actions.secondaryLabel,
            onClick = actions.onSecondaryClick,
            scale = scale,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200 * scale))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(role = Role.Button, onClick = actions.onShareClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = actions.shareLabel,
            style = GamssTheme.typography.body5Medium,
            color = GamssTheme.colors.gray600,
        )
        Spacer(modifier = Modifier.width(GamssTheme.spacing.spacing025 * scale))
        Icon(
            painter = painterResource(GamssIcons.RightChevron),
            contentDescription = null,
            tint = GamssTheme.colors.gray600,
            modifier = Modifier.size(GamssTheme.spacing.spacing300 * scale),
        )
    }
}

/**
 * 공유 이미지 아래쪽. 시안(Figma 4243:18230)대로 버튼이 있던 자리에 GAMSS 워드마크를 둔다.
 *
 * 카드 안은 [LightGamssColors] 로 고정이라 여기서 읽는 보라도 라이트 값으로 정해진다.
 */
@Composable
private fun ColumnScope.CardBrandFooter() {
    Image(
        painter = painterResource(R.drawable.ic_gamss_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(GamssTheme.colors.purple),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(width = BrandLogoWidth, height = BrandLogoHeight),
    )
}

@Composable
fun ColumnScope.GamssEmotionCardContent(
    character: GamssEmotionCardCharacter,
    scale: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(modifier = Modifier.height(DateToCharacterGap * scale))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CharacterImageHeight * scale),
    ) {
        GamssEmotionCardCharacterImage(character = character)
    }
    Spacer(modifier = Modifier.height(CharacterToDividerGap * scale))
    GamssCardDashedDivider()
    Spacer(modifier = Modifier.height(DividerToContentGap * scale))
    content()
}

/**
 * 카드 안을 가로로 끊어 주는 점선. 부모가 준 폭을 그대로 채우므로 카드 콘텐츠 열 안에 둬야
 * 좌우 여백이 시안과 맞는다. 카드 밖에서 쓰면 화면 끝까지 뻗는다.
 */
@Composable
fun GamssCardDashedDivider(modifier: Modifier = Modifier) {
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
 * 이미지 카드 안에 대화 로그를 배치하는 카드 퍼사드. 카드가 고정 크기라 [content] 에 날짜 아래 남은
 * 높이를 모두 넘긴다. 호출부는 그 안에서 스크롤만 붙이면 된다.
 *
 * 대화 로그는 카드 안에서 스크롤되어 넘칠 일이 없으므로 폰트 확대 상한을 두지 않습니다.
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
        capFontScale = false,
        topEndAction = topEndAction,
    ) { scale ->
        Spacer(modifier = Modifier.height(DateToChattingGap * scale))
        Column(
            modifier = Modifier
                .weight(1f)
                // 카드가 아래쪽 테두리 여백을 이미 두므로 시안 40 에서 그만큼 뺍니다.
                .padding(bottom = (GamssTheme.spacing.spacing700 - CardBorderInset) * scale),
            content = content,
        )
    }
}

@Composable
private fun EmotionCardPreviewContent(
    description: String = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
) {
    GamssEmotionCard(
        date = "26.08.03",
        character = GamssEmotionCardCharacter.ANGER,
        title = "오늘 화~나네",
        description = description,
        footer = GamssEmotionCardFooter.Actions(
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
    scale: Float,
    modifier: Modifier = Modifier,
) {
    GamssOutlinedCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = GamssTheme.colors.gray900,
        contentPadding = PaddingValues(vertical = GamssTheme.spacing.spacing200 * scale),
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
        Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing300)) {
            GamssSentChatBubble(
                message = "안녕하세요ㅁㅇㄹㅁㅇㄹㅁㅇㄹ",
                time = "오후 1:37",
                modifier = Modifier.align(Alignment.End),
                oppositeWallGap = GamssChatBubbleDefaults.CardOppositeWallGap,
            )
            GamssReceivedChatBubble(
                sender = ChatSender(name = "기쁨이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
                oppositeWallGap = GamssChatBubbleDefaults.CardOppositeWallGap,
            )
            GamssReceivedChatBubble(
                sender = ChatSender(name = "슬픔이"),
                message = "안녕! 오늘도 행복한 하루~!",
                time = "오후 1:38",
                oppositeWallGap = GamssChatBubbleDefaults.CardOppositeWallGap,
            )
        }
    }
}

private val CardAspectRatio = GamssCardDefaults.Width / GamssCardDefaults.Height

/** 세로가 가장 빡빡한 채팅 접기 화면이 정한 값입니다. 공유 줄을 켜면 다시 재야 합니다. */
private const val MAX_FONT_SCALE_AT_DESIGN_WIDTH = 1.3f

/** 손그림 테두리가 카드 박스 안쪽 약 13dp 에 그려져 있어 아래쪽은 이만큼 비웁니다. */
private val CardBorderInset = 13.dp

private val DateToCharacterGap = 18.dp

/** Figma Card_Chatting 가이드(3264:7548)의 Date - chat 간격. */
private val DateToChattingGap = 24.dp
private val CharacterImageHeight = 156.dp

// 점선 관련 값은 Figma Card(3557:5286)의 Divider 기준이다. 점선은 콘텐츠 열 전체 폭(270)을
// 쓰고 위아래 22 씩 띄우며, 캐릭터 이미지와는 16 만 띄운다.
private val CharacterToDividerGap = 16.dp
private val DividerToContentGap = 22.dp

/** 공유 이미지 워드마크 크기. 시안 94x28. */
private val BrandLogoWidth = 94.dp
private val BrandLogoHeight = 28.dp
private val CardDividerThickness = 1.3.dp
private val CardDividerDashLength = 6.dp
private val CardDividerDashGap = 4.dp
