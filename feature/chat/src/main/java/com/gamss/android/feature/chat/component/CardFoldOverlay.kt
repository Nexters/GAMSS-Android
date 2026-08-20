package com.gamss.android.feature.chat.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.common.util.formatCardDate
import com.gamss.android.core.designsystem.card.GamssCardDashedDivider
import com.gamss.android.core.designsystem.card.GamssEmotionCardContent
import com.gamss.android.core.designsystem.card.GamssImageCard
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.cardTitleRes
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.chat.CARD_FOLD_BIN_ASPECT_RATIO
import com.gamss.android.feature.chat.CARD_FOLD_SUMMARY_MAX_LINES
import com.gamss.android.feature.chat.CardFoldArrowBinGap
import com.gamss.android.feature.chat.CardFoldArrowSize
import com.gamss.android.feature.chat.CardFoldDividerToQuestionGap
import com.gamss.android.feature.chat.CardFoldGuideGap
import com.gamss.android.feature.chat.CardFoldQuestionToTapGuideGap
import com.gamss.android.feature.chat.CardFoldSkipCenteringInset
import com.gamss.android.feature.chat.CardFoldSkipIconSize
import com.gamss.android.feature.chat.CardFoldSkipTouchSize
import com.gamss.android.feature.chat.CardFoldStage
import com.gamss.android.feature.chat.CardFoldSummaryToDividerGap
import com.gamss.android.feature.chat.CardFoldTapGuideSize
import com.gamss.android.feature.chat.R
import java.time.LocalDate
import com.gamss.android.core.designsystem.R as DesignSystemR

/**
 * 카드를 눌러 두 번 접고, 접힌 종이를 아래 통으로 끌어내려 버리는 연출.
 *
 * 배경 dim 은 [Dialog] 창이 기본으로 그려 주므로 여기서 따로 그리지 않는다. 카드는 이미 서버에
 * 만들어져 되돌릴 수 없으므로 [Dialog] 의 취소 경로(뒤로가기·바깥 탭)는 모두 닫아 두고, 대신
 * 카드 우상단 닫기 버튼으로만 연출을 건너뛴다.
 *
 * @param onSkip 접기를 건너뛰고 화면을 벗어난다. 카드는 이미 기록에 남아 있어 버리는 동작과
 *  결과가 같고, 연출만 생략한다.
 */
@Composable
internal fun CardFoldOverlay(
    card: Card,
    foldStage: CardFoldStage,
    onFoldTap: () -> Unit,
    onSkip: () -> Unit,
    onDiscard: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            // 통은 화면 맨 아래에 붙어야 한다. 기본값이면 창이 시스템 바를 피해서 그려져
            // 내비게이션 바 자리만큼 통이 떠 보인다. 시안의 874 프레임도 상태바와 홈
            // 인디케이터를 포함한 높이다.
            decorFitsSystemWindows = false,
        ),
    ) {
        CardFoldContent(
            card = card,
            foldStage = foldStage,
            onFoldTap = onFoldTap,
            onSkip = onSkip,
            onDiscard = onDiscard,
        )
    }
}

@Composable
private fun CardFoldContent(
    card: Card,
    foldStage: CardFoldStage,
    onFoldTap: () -> Unit,
    onSkip: () -> Unit,
    onDiscard: () -> Unit,
) {
    val folded = foldStage.next == null
    val binReady = rememberBinReady(folded)
    val hints = rememberCardFoldHints(folded)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val metrics = rememberCardFoldMetrics(maxWidth, maxHeight, foldStage)
        val drag = rememberCardFoldDragState(metrics.travel, onDiscard)

        // Figma 에서 세 단계가 모두 같은 중심선에 놓인다.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(metrics.paperSize),
        ) {
            DiscardGuide(
                // 안내와 화살표는 힌트라 끌기 시작하면 사라진다. 통은 남는다.
                alpha = { hints.guide.value * (1f - drag.dragFraction()) },
                gap = CardFoldGuideGap * metrics.scale,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = drag.offset.floatValue }
                    .noRippleClickableIfNotNull(onFoldTap.takeIf { !folded })
                    .discardDraggable(state = drag, enabled = binReady),
            ) {
                StagePaper(stage = foldStage, card = card, onSkip = onSkip)
            }
        }

        // 종이보다 앞에 그린다. Figma 도 화살표를 종이 위에 얹고, 위쪽이 투명해 종이를 가리지 않는다.
        DiscardArrow(
            alpha = { hints.arrow.value * (1f - drag.dragFraction()) },
            size = CardFoldArrowSize * metrics.scale,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = metrics.binHeight + CardFoldArrowBinGap),
        )

        // 종이보다 뒤에 그리면 통 앞면에 가려 파묻히는 모습이 안 나온다.
        DiscardBin(
            alpha = { hints.bin.value },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StagePaper(stage: CardFoldStage, card: Card, onSkip: () -> Unit) {
    when (stage) {
        CardFoldStage.Unfolded -> UnfoldedPaper(card = card, onSkip = onSkip)

        CardFoldStage.FoldedOnce -> FoldedPaperImage(DesignSystemR.drawable.img_paper_folded_once)
        CardFoldStage.FoldedTwice -> FoldedPaperImage(DesignSystemR.drawable.img_paper_folded_twice)
    }
}

@Composable
private fun UnfoldedPaper(card: Card, onSkip: () -> Unit) {
    GamssImageCard(
        date = formatCardDate(card.date),
        topEndAction = { FoldSkipButton(onClick = onSkip) },
    ) {
        GamssEmotionCardContent(
            character = card.character.toGamssEmotionCardCharacter(),
            showDivider = true,
        ) {
            Text(
                text = stringResource(card.character.cardTitleRes()),
                modifier = Modifier.fillMaxWidth(),
                style = GamssTheme.typography.title2,
                color = GamssTheme.colors.gray950,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
            CardSummaryText(summary = card.summary, modifier = Modifier.weight(1f, fill = false))
            FoldPromptFooter()
        }
    }
}

/**
 * 카드 높이는 폭에 비례해 줄어드는데 안쪽 여백과 캐릭터 그림은 절대 dp 라, 좁은 기기에서 세 줄을
 * 다 쓰면 아래 점선과 안내가 카드 밖으로 밀려 잘린다. 밀리는 쪽이 아니라 요약이 양보해야 한다.
 */
@Composable
private fun CardSummaryText(summary: String, modifier: Modifier = Modifier) {
    Text(
        text = summary,
        modifier = modifier.fillMaxWidth(),
        style = GamssTheme.typography.body4Regular,
        color = GamssTheme.colors.gray800,
        textAlign = TextAlign.Center,
        maxLines = CARD_FOLD_SUMMARY_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 카드 밑에 겹쳐 놓지 않고 흐름에 넣어야 본문 길이가 달라져도 시안의 간격이 유지된다. */
@Composable
private fun ColumnScope.FoldPromptFooter() {
    Spacer(modifier = Modifier.height(CardFoldSummaryToDividerGap))
    GamssCardDashedDivider()
    Spacer(modifier = Modifier.height(CardFoldDividerToQuestionGap))
    Text(
        text = stringResource(R.string.chat_room_card_fold_discard_question),
        modifier = Modifier.fillMaxWidth(),
        // 시안은 Pretendard Light 12/20 이지만 타이포 토큰에 Light 가 없어 가장 가까운
        // 12sp Regular 을 쓴다.
        style = GamssTheme.typography.body5Regular,
        color = GamssTheme.colors.gray800,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(CardFoldQuestionToTapGuideGap))
    Image(
        painter = painterResource(DesignSystemR.drawable.img_paper_fold_guide),
        contentDescription = stringResource(R.string.chat_room_card_fold_guide),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(CardFoldTapGuideSize),
    )
}

@Composable
private fun FoldedPaperImage(@DrawableRes resId: Int) {
    FoldImage(
        resId = resId,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * 연출을 건너뛰는 우상단 닫기 버튼. 보관함 카드의 닫기와 같은 모양이지만, 두 화면의 카드 UI 가
 * 달라 각자 두고 함께 움직이지 않게 한다.
 */
@Composable
private fun FoldSkipButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = CardFoldSkipCenteringInset, y = -CardFoldSkipCenteringInset)
            .size(CardFoldSkipTouchSize)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GamssIcons.Close),
            contentDescription = stringResource(R.string.chat_room_card_fold_skip_description),
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(CardFoldSkipIconSize),
        )
    }
}

@Composable
private fun DiscardGuide(alpha: () -> Float, gap: Dp, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.chat_room_card_fold_discard_guide),
        style = GamssTheme.typography.subtitle1,
        color = GamssTheme.colors.white,
        textAlign = TextAlign.Center,
        modifier = modifier
            .anchorAbove(gap)
            .fadingAlpha(alpha),
    )
}

/** 위쪽이 투명한 그라데이션이라 종이 위에 겹쳐도 가리지 않는다. */
@Composable
private fun DiscardArrow(alpha: () -> Float, size: DpSize, modifier: Modifier = Modifier) {
    FoldImage(
        resId = DesignSystemR.drawable.img_paper_discard_arrow,
        alpha = alpha,
        modifier = modifier.size(size),
    )
}

/** 높이를 dp 로 박지 않고 폭에 비율을 걸어 뽑는다. 손그림 테두리라 폭만 늘리면 눌린 모습이 난다. */
@Composable
private fun DiscardBin(alpha: () -> Float, modifier: Modifier = Modifier) {
    FoldImage(
        resId = DesignSystemR.drawable.img_paper_discard_bin,
        alpha = alpha,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_FOLD_BIN_ASPECT_RATIO),
    )
}

/** 연출에 쓰는 그림은 모두 장식이라 대체 텍스트를 두지 않는다. */
@Composable
private fun FoldImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: () -> Float = Opaque,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.fadingAlpha(alpha),
    )
}

/** 진행률을 draw 단계에서만 읽는다. 컴포지션 본문에서 읽으면 배어 나오는 동안 매 프레임 다시 그려진다. */
private fun Modifier.fadingAlpha(alpha: () -> Float) = graphicsLayer { this.alpha = alpha() }

/** 붙은 자리의 위쪽 바깥으로 옮긴다. 자기 높이를 알아야 올릴 수 있어 배치 단계에서 옮긴다. */
private fun Modifier.anchorAbove(gap: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(x = 0, y = -(placeable.height + gap.roundToPx()))
    }
}

private fun Modifier.discardDraggable(state: CardFoldDragState, enabled: Boolean) = draggable(
    state = state.draggableState,
    orientation = Orientation.Vertical,
    enabled = enabled && !state.discarding,
    onDragStopped = { state.onDragStopped() },
)

private val Opaque: () -> Float = { 1f }

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
private annotation class CardFoldFramePreview

@CardFoldFramePreview
@Suppress("UnusedPrivateMember")
@Composable
private fun CardFoldUnfoldedPreview() = CardFoldPreview(CardFoldStage.Unfolded)

@CardFoldFramePreview
@Suppress("UnusedPrivateMember")
@Composable
private fun CardFoldOncePreview() = CardFoldPreview(CardFoldStage.FoldedOnce)

@CardFoldFramePreview
@Suppress("UnusedPrivateMember")
@Composable
private fun CardFoldTwicePreview() = CardFoldPreview(CardFoldStage.FoldedTwice)

@Composable
private fun CardFoldPreview(foldStage: CardFoldStage) {
    GamssTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GamssTheme.colors.gray900),
        ) {
            CardFoldContent(
                card = PreviewCard,
                foldStage = foldStage,
                onFoldTap = {},
                onSkip = {},
                onDiscard = {},
            )
        }
    }
}

private val PreviewCard = Card(
    id = 1L,
    conversationId = 1L,
    character = EmotionCharacter.ANGER,
    emotionLabel = "분노",
    summary = "오늘은 유난히 화가 많이 났던 하루였어요.",
    message = "그럴 수 있어요.",
    date = LocalDate.of(2026, 8, 19),
)
