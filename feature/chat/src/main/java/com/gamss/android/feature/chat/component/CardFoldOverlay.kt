package com.gamss.android.feature.chat.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.gamss.android.feature.chat.CARD_FOLD_ARROW_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_BIN_ASPECT_RATIO
import com.gamss.android.feature.chat.CARD_FOLD_BIN_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_BIN_SINK_FRACTION
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_HOLD_MS
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_SINK_MS
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_THRESHOLD
import com.gamss.android.feature.chat.CARD_FOLD_GUIDE_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_HINT_FADE_MS
import com.gamss.android.feature.chat.CardFoldArrowBinGap
import com.gamss.android.feature.chat.CardFoldArrowSize
import com.gamss.android.feature.chat.CardFoldDesignWidth
import com.gamss.android.feature.chat.CardFoldDividerToQuestionGap
import com.gamss.android.feature.chat.CardFoldGuideGap
import com.gamss.android.feature.chat.CardFoldQuestionToTapGuideGap
import com.gamss.android.feature.chat.CardFoldStage
import com.gamss.android.feature.chat.CardFoldSummaryToDividerGap
import com.gamss.android.feature.chat.CardFoldTapGuideSize
import com.gamss.android.feature.chat.R
import com.gamss.android.feature.chat.paperSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    val dragOffset = remember { mutableFloatStateOf(0f) }
    // 다 접혔는지가 접기와 끌기를 가른다. 더 접을 게 없으면 이제 통으로 내리는 단계다.
    val folded = foldStage.next == null

    // 통이 뜨기 전에는 못 끌게 막는다. 다 접자마자 그대로 내려 버리면 종이가 빈 자리로 가라앉고,
    // 어디에 버린 건지 못 본 채 화면이 넘어간다.
    var binShown by remember { mutableStateOf(false) }
    LaunchedEffect(folded) {
        binShown = false
        if (folded) {
            delay(CARD_FOLD_BIN_DELAY_MS.toLong())
            binShown = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 시안은 402dp 폭 기준이다. 좁은 기기에서 종이와 힌트를 폭 비율만큼 함께 줄여 좌우 여백
        // 비율을 지킨다. 통은 폭을 채우고 높이를 비율로 뽑으므로 이미 같은 비율로 줄어든다.
        val designScale = (maxWidth / CardFoldDesignWidth).coerceAtMost(1f)
        val paperSize = foldStage.paperSize * designScale

        // 통은 창 아래에 붙고 종이는 창 중심에 붙으므로 내려갈 거리가 이 창의 크기에서 바로 나온다.
        // 화면 높이 상수로 되짚지 않아 인셋이나 폰트 배율이 바뀌어도 어긋나지 않는다.
        val binHeight = maxWidth / CARD_FOLD_BIN_ASPECT_RATIO
        val travel = with(LocalDensity.current) { discardTravel(maxHeight, binHeight, paperSize.height) }

        // 진행률은 graphicsLayer 안에서만 불러 쓴다. 컴포지션 본문에서 읽으면 끄는 동안 매
        // 프레임 카드까지 다시 그려진다.
        val dragFraction = {
            val distance = travel.dragDistancePx
            if (distance > 0f) (dragOffset.floatValue / distance).coerceIn(0f, 1f) else 0f
        }
        // 접힘 자체는 툭 바뀌지만, 다 접힌 뒤에는 통 → 안내 → 화살표가 하나씩 배어 나온다. 셋 다
        // 버리는 동안에도 남아야 하므로, 끌 수 있는지가 아니라 단계로 판단한다.
        val binProgress = fadeInAfter(folded, CARD_FOLD_BIN_DELAY_MS, label = "cardFoldBin")
        val guideProgress = fadeInAfter(folded, CARD_FOLD_GUIDE_DELAY_MS, label = "cardFoldGuide")
        val arrowProgress = fadeInAfter(folded, CARD_FOLD_ARROW_DELAY_MS, label = "cardFoldArrow")

        // 종이는 화면 중심에 고정한다. Figma 에서 세 단계가 모두 같은 중심선에 놓인다.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(paperSize),
        ) {
            DiscardGuide(
                // 안내와 화살표는 힌트라 끌기 시작하면 사라진다. 통은 남는다.
                alpha = { guideProgress.value * (1f - dragFraction()) },
                gap = CardFoldGuideGap * designScale,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragOffset.floatValue }
                    .noRippleClickableIfNotNull(onFoldTap.takeIf { !folded })
                    .discardDraggable(
                        offset = dragOffset,
                        travel = travel,
                        enabled = binShown,
                        onDiscard = onDiscard,
                    ),
            ) {
                StagePaper(stage = foldStage, card = card, onSkip = onSkip)
            }
        }

        // 종이보다 앞에 그린다. Figma 도 화살표를 종이 위에 얹고, 위쪽이 투명해 종이를 가리지 않는다.
        DiscardArrow(
            alpha = { arrowProgress.value * (1f - dragFraction()) },
            size = CardFoldArrowSize * designScale,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = binHeight + CardFoldArrowBinGap),
        )

        // 종이보다 뒤에 그리면 통 앞면에 가려 파묻히는 모습이 안 나온다.
        DiscardBin(
            alpha = { binProgress.value },
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

/** 아직 안 접힌 카드. 본문 아래로 점선과 안내 문구, 접으라는 손글씨가 이어 붙는다. */
@Composable
private fun UnfoldedPaper(card: Card, onSkip: () -> Unit) {
    GamssImageCard(
        date = card.date.format(CardFoldDateFormatter),
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
            Text(
                text = card.summary,
                // 남는 자리만 차지하고 모자라면 줄인다. 카드 높이는 폭에 비례해 줄어드는데 안쪽
                // 여백과 캐릭터 그림은 절대 dp 라, 좁은 기기에서 세 줄을 다 쓰면 아래 점선과
                // 안내가 카드 밖으로 밀려 잘린다. 밀리는 쪽이 아니라 요약이 양보해야 한다.
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                style = GamssTheme.typography.body4Regular,
                color = GamssTheme.colors.gray800,
                textAlign = TextAlign.Center,
                maxLines = SUMMARY_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            // 본문 아래로 점선을 한 번 더 긋고, 그 아래에 버릴지 묻는 문구와 접으라는
            // 손글씨를 둔다. 카드 밑에 겹쳐 놓지 않고 흐름에 넣어야 본문 길이가 달라져도
            // 시안의 간격이 유지된다.
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
    }
}

@Composable
private fun FoldedPaperImage(resId: Int) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * 연출을 건너뛰는 우상단 닫기 버튼. 보관함 카드의 닫기와 같은 모양이지만, 두 화면의 카드 UI 가
 * 달라 각자 두고 함께 움직이지 않게 한다.
 *
 * 카드가 액션 슬롯을 Figma 값(우상단 28dp)에 맞춰 두므로 아이콘은 슬롯 좌상단에 딱 붙어야 한다.
 * 그런데 터치 영역을 아이콘보다 크게 잡으면 그 차이만큼 아이콘이 안쪽으로 밀리므로,
 * [SkipButtonCenteringInset] 만큼 되돌려 아이콘을 시안 위치로 보낸다.
 */
@Composable
private fun FoldSkipButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = SkipButtonCenteringInset, y = -SkipButtonCenteringInset)
            .size(SkipButtonTouchSize)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GamssIcons.Close),
            contentDescription = stringResource(R.string.chat_room_card_fold_skip_description),
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(SkipIconSize),
        )
    }
}

/** 통으로 내리라는 안내. 종이 위쪽 바깥에 붙는다. */
@Composable
private fun DiscardGuide(alpha: () -> Float, gap: Dp, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.chat_room_card_fold_discard_guide),
        style = GamssTheme.typography.subtitle1,
        color = GamssTheme.colors.white,
        textAlign = TextAlign.Center,
        modifier = modifier
            .anchorAbove(gap)
            .graphicsLayer { this.alpha = alpha() },
    )
}

/** 아래로 끌어내리라는 힌트. 위쪽이 투명한 그라데이션이라 종이 위에 겹쳐도 가리지 않는다. */
@Composable
private fun DiscardArrow(alpha: () -> Float, size: DpSize, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(DesignSystemR.drawable.img_paper_discard_arrow),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha() },
    )
}

/**
 * 화면 아래에 걸친 쓰레기통.
 *
 * 높이를 dp 로 박지 않고 폭에 비율을 걸어 뽑는다. 손그림 테두리라 폭만 늘리면 눌린 모습이 난다.
 */
@Composable
private fun DiscardBin(alpha: () -> Float, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(DesignSystemR.drawable.img_paper_discard_bin),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_FOLD_BIN_ASPECT_RATIO)
            .graphicsLayer { this.alpha = alpha() },
    )
}

/** 붙은 자리의 위쪽 바깥으로 옮긴다. 자기 높이를 알아야 올릴 수 있어 배치 단계에서 옮긴다. */
private fun Modifier.anchorAbove(gap: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(x = 0, y = -(placeable.height + gap.roundToPx()))
    }
}

/**
 * 통까지 끌어내리는 제스처. 절반 넘게 내려놓으면 손을 떼도 통 뒤로 완전히 넣은 뒤 [onDiscard] 를
 * 부르고, 못 미치면 제자리로 돌아온다.
 *
 * 끄는 동안은 [offset] 을 직접 바꾼다. Animatable 을 두고 델타마다 코루틴을 열어 snapTo 를 부르면,
 * 손을 뗀 뒤 시작한 가라앉기 애니메이션을 뒤늦게 도착한 snapTo 가 Animatable 의 MutatorMutex 로
 * 취소한다(같은 우선순위는 나중 것이 이긴다). 그러면 [onDiscardStart] 만 불린 채 [onDiscard] 가
 * 영영 안 불려, 나갈 길이 없는 화면이 남는다.
 *
 * 가라앉기도 제스처 콜백이 아니라 컴포지션 스코프에서 돌린다. 버리기가 확정된 뒤의 연출은
 * 제스처보다 오래 살아야 한다.
 */
@Composable
private fun Modifier.discardDraggable(
    offset: MutableFloatState,
    travel: DiscardTravel,
    enabled: Boolean,
    onDiscard: () -> Unit,
): Modifier {
    val discardScope = rememberCoroutineScope()
    // 가라앉는 동안 다시 잡히지 않게 잠근다. 잠금을 밖으로 올리면 이 모디파이어를 다시 열어 줄
    // 콜백까지 화면이 들고 있어야 한다.
    var discarding by remember { mutableStateOf(false) }
    return draggable(
        // 통 방향으로만 움직이고 통을 지나 더 내려가지는 않는다.
        state = rememberDraggableState { delta ->
            offset.floatValue = (offset.floatValue + delta).coerceIn(0f, travel.dragDistancePx)
        },
        orientation = Orientation.Vertical,
        enabled = enabled && !discarding,
        onDragStopped = {
            if (reachedBin(offset.floatValue, travel.dragDistancePx)) {
                discarding = true
                discardScope.launch {
                    animate(
                        initialValue = offset.floatValue,
                        targetValue = travel.swallowDistancePx,
                        animationSpec = DiscardSinkSpec,
                    ) { value, _ -> offset.floatValue = value }
                    delay(CARD_FOLD_DISCARD_HOLD_MS)
                    onDiscard()
                }
            } else {
                animate(offset.floatValue, 0f, animationSpec = spring()) { value, _ ->
                    offset.floatValue = value
                }
            }
        },
    )
}

/**
 * 종이가 통까지 움직일 거리.
 *
 * @property dragDistancePx 손으로 끌 수 있는 끝. 종이 아래끝이 입구에 닿는 지점이다. 더 끌리게
 *  두면 종이가 통 뒤로 숨어 어디까지 왔는지 안 보인다.
 * @property swallowDistancePx 손을 뗀 뒤 가라앉는 끝. 종이 위끝까지 입구 아래로 넣어 통 뒤로
 *  완전히 감춘다.
 */
private data class DiscardTravel(val dragDistancePx: Float, val swallowDistancePx: Float)

/** 통은 창 아래에, 종이는 창 중심에 붙으므로 두 거리가 창 크기에서 바로 나온다. */
private fun Density.discardTravel(windowHeight: Dp, binHeight: Dp, paperHeight: Dp): DiscardTravel {
    val sinkTarget = windowHeight - binHeight * (1f - CARD_FOLD_BIN_SINK_FRACTION)
    return DiscardTravel(
        dragDistancePx = (sinkTarget - (windowHeight + paperHeight) / 2).coerceAtLeast(0.dp).toPx(),
        swallowDistancePx = (sinkTarget - (windowHeight - paperHeight) / 2).coerceAtLeast(0.dp).toPx(),
    )
}

/** 마지막으로 접은 뒤 [delayMillis] 만큼 쉬었다 배어 나온다. */
@Composable
private fun fadeInAfter(visible: Boolean, delayMillis: Int, label: String): State<Float> =
    animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(CARD_FOLD_HINT_FADE_MS, delayMillis = delayMillis),
        label = label,
    )

/** 손을 뗀 자리가 통에 넣기로 볼 만큼 내려왔는지. */
private fun reachedBin(dragOffsetPx: Float, dragDistancePx: Float): Boolean =
    dragDistancePx > 0f && dragOffsetPx >= dragDistancePx * CARD_FOLD_DISCARD_THRESHOLD

private val DiscardSinkSpec = tween<Float>(CARD_FOLD_DISCARD_SINK_MS, easing = FastOutSlowInEasing)

/** [com.gamss.android.core.designsystem.card.GamssEmotionCard] 의 본문 상한과 같은 값이다. */
private const val SUMMARY_MAX_LINES = 3

private val SkipIconSize = 20.dp
private val SkipButtonTouchSize = 48.dp
private val SkipButtonCenteringInset = (SkipButtonTouchSize - SkipIconSize) / 2
private val CardFoldDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM.dd")

@Preview(name = "Unfolded", showBackground = true, widthDp = 402, heightDp = 874)
@Suppress("UnusedPrivateMember")
@Composable
private fun CardFoldUnfoldedPreview() = CardFoldPreview(CardFoldStage.Unfolded)

@Preview(name = "FoldedOnce", showBackground = true, widthDp = 402, heightDp = 874)
@Suppress("UnusedPrivateMember")
@Composable
private fun CardFoldOncePreview() = CardFoldPreview(CardFoldStage.FoldedOnce)

@Preview(name = "FoldedTwice", showBackground = true, widthDp = 402, heightDp = 874)
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
