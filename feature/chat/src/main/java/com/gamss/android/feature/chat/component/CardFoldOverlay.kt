package com.gamss.android.feature.chat.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.gamss.android.feature.chat.CARD_FOLD_BIN_SINK_FRACTION
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_HOLD_MS
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_THRESHOLD
import com.gamss.android.feature.chat.CARD_FOLD_STEP_DURATION_MS
import com.gamss.android.feature.chat.CardFoldArrowBinGap
import com.gamss.android.feature.chat.CardFoldArrowSize
import com.gamss.android.feature.chat.CardFoldDesignWidth
import com.gamss.android.feature.chat.CardFoldGuideGap
import com.gamss.android.feature.chat.CardFoldStage
import com.gamss.android.feature.chat.CardFoldTapGuideBottomInset
import com.gamss.android.feature.chat.CardFoldTapGuideSize
import com.gamss.android.feature.chat.R
import com.gamss.android.feature.chat.paperSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
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
    // 단계를 실수로 표현해 두 단계 사이를 오갈 수 있게 한다. 크기와 겹침 정도를 이 값 하나로 뽑는다.
    val foldProgress = remember { Animatable(foldStage.ordinal.toFloat()) }
    LaunchedEffect(foldStage) {
        foldProgress.animateTo(
            foldStage.ordinal.toFloat(),
            tween(CARD_FOLD_STEP_DURATION_MS, easing = FastOutSlowInEasing),
        )
    }

    val dragOffset = remember { Animatable(0f) }
    // draggable 의 델타 콜백은 suspend 가 아니라 여기서 코루틴을 열어 snapTo 를 부른다.
    val dragScope = rememberCoroutineScope()
    var isDiscarding by remember { mutableStateOf(false) }

    val canFold = foldStage.next != null && !isDiscarding
    val canDrag = foldStage.next == null && !isDiscarding

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 시안은 402dp 폭 기준이다. 좁은 기기에서 종이와 힌트를 폭 비율만큼 함께 줄여 좌우 여백
        // 비율을 지킨다. 통은 폭을 채우고 높이를 비율로 뽑으므로 이미 같은 비율로 줄어든다.
        val designScale = (maxWidth / CardFoldDesignWidth).coerceAtMost(1f)
        val paperSize = lerpPaperSize(foldProgress.value) * designScale

        // 통은 창 아래에 붙고 종이는 창 중심에 붙으므로 내려갈 거리가 이 창의 크기에서 바로 나온다.
        // 화면 높이 상수로 되짚지 않아 인셋이나 폰트 배율이 바뀌어도 어긋나지 않는다.
        val binHeight = maxWidth / CARD_FOLD_BIN_ASPECT_RATIO
        val paperRestBottom = (maxHeight + paperSize.height) / 2
        val sinkTarget = maxHeight - binHeight * (1f - CARD_FOLD_BIN_SINK_FRACTION)
        val dragDistancePx = with(LocalDensity.current) {
            (sinkTarget - paperRestBottom).coerceAtLeast(0.dp).toPx()
        }

        // 진행률은 graphicsLayer 안에서만 불러 쓴다. 컴포지션 본문에서 dragOffset.value 를 읽으면
        // 끄는 동안 매 프레임 카드까지 다시 그려진다.
        val dragFraction = { if (dragDistancePx > 0f) (dragOffset.value / dragDistancePx).coerceIn(0f, 1f) else 0f }
        // 통과 화살표, 버리기 안내는 마지막 접힘과 함께 배어 나온다. 끌기 시작하면 다시 사라진다.
        val discardProgress = (foldProgress.value - CardFoldStage.FoldedOnce.ordinal).coerceIn(0f, 1f)
        val hintAlpha = { discardProgress * (1f - dragFraction()) }

        // 종이는 화면 중심에 고정한다. Figma 에서 세 단계가 모두 같은 중심선에 놓인다.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(paperSize),
        ) {
            DiscardGuide(
                alpha = hintAlpha,
                gap = CardFoldGuideGap * designScale,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            FoldingPaper(
                card = card,
                paperSize = paperSize,
                designScale = designScale,
                foldProgress = foldProgress.value,
                foldStage = foldStage,
                onSkip = onSkip,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragOffset.value }
                    .noRippleClickableIfNotNull(onFoldTap.takeIf { canFold })
                    .draggable(
                        state = rememberDraggableState { delta ->
                            dragScope.launch { dragOffset.updateBounded(delta, dragDistancePx) }
                        },
                        orientation = Orientation.Vertical,
                        enabled = canDrag,
                        onDragStopped = {
                            // 가라앉는 동안 다시 잡히지 않게, 애니메이션 전에 잠근다.
                            val discarding = dragOffset.reachedBin(dragDistancePx)
                            isDiscarding = discarding
                            if (discarding) {
                                dragOffset.animateTo(dragDistancePx, DiscardSinkSpec)
                                delay(CARD_FOLD_DISCARD_HOLD_MS)
                                onDiscard()
                            } else {
                                dragOffset.animateTo(0f, spring())
                            }
                        },
                    ),
            )
        }

        // 종이보다 앞에 그린다. Figma 도 화살표를 종이 위에 얹고, 위쪽이 투명해 종이를 가리지 않는다.
        DiscardArrow(
            alpha = hintAlpha,
            size = CardFoldArrowSize * designScale,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = binHeight + CardFoldArrowBinGap),
        )

        // 종이보다 뒤에 그리면 통 앞면에 가려 파묻히는 모습이 안 나온다.
        DiscardBin(
            alpha = { discardProgress },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 세 단계 그림을 겹쳐 두고 [foldProgress] 에 가까운 것만 보여 준다. 각 그림은 자기 원본 크기로
 * 배치한 뒤 [paperSize] 까지 눌러서, 카드가 접히며 찌그러지는 모습이 그대로 나온다. 접히는
 * 기준선은 가운데다.
 *
 * 원본 크기는 requiredSize 로 준다. size 로 주면 부모의 최대 크기에 먼저 잘려, 단계 사이에서
 * 레이아웃과 scale 이 이중으로 줄어든다.
 */
@Composable
private fun FoldingPaper(
    card: Card,
    paperSize: DpSize,
    designScale: Float,
    foldProgress: Float,
    foldStage: CardFoldStage,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CardFoldStage.entries.forEach { stage ->
            val alpha = (1f - abs(foldProgress - stage.ordinal)).coerceIn(0f, 1f)
            if (alpha == 0f) return@forEach

            val natural = stage.paperSize * designScale
            Box(
                modifier = Modifier
                    .requiredSize(natural)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = paperSize.width / natural.width
                        scaleY = paperSize.height / natural.height
                    },
            ) {
                StagePaper(
                    stage = stage,
                    card = card,
                    // 흐려지는 중인 카드의 닫기 버튼까지 눌리지 않게, 지금 단계일 때만 연다.
                    onSkip = onSkip.takeIf { stage == foldStage },
                )
            }
        }
    }
}

@Composable
private fun StagePaper(stage: CardFoldStage, card: Card, onSkip: (() -> Unit)?) {
    when (stage) {
        CardFoldStage.Unfolded -> Box(modifier = Modifier.fillMaxSize()) {
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
                        modifier = Modifier.fillMaxWidth(),
                        style = GamssTheme.typography.body4Regular,
                        color = GamssTheme.colors.gray800,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Figma 는 접기 안내를 손글씨 그림으로 준다. 카드 아래쪽에 겹쳐 놓는다.
            Image(
                painter = painterResource(DesignSystemR.drawable.img_paper_fold_guide),
                contentDescription = stringResource(R.string.chat_room_card_fold_guide),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = CardFoldTapGuideBottomInset)
                    .size(CardFoldTapGuideSize),
            )
        }

        CardFoldStage.FoldedOnce -> FoldedPaperImage(DesignSystemR.drawable.img_paper_folded_once)
        CardFoldStage.FoldedTwice -> FoldedPaperImage(DesignSystemR.drawable.img_paper_folded_twice)
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
private fun FoldSkipButton(onClick: (() -> Unit)?) {
    if (onClick == null) return

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

/** 앞 단계 크기에서 다음 단계 크기로 선형 보간한다. */
private fun lerpPaperSize(foldProgress: Float): DpSize {
    val stages = CardFoldStage.entries
    val from = stages[foldProgress.toInt().coerceIn(0, stages.lastIndex)]
    val to = stages[(foldProgress.toInt() + 1).coerceIn(0, stages.lastIndex)]
    return lerp(from.paperSize, to.paperSize, foldProgress - foldProgress.toInt())
}

/** 통 방향으로만 움직이고 통을 지나 더 내려가지는 않는다. */
private suspend fun Animatable<Float, *>.updateBounded(delta: Float, maxOffset: Float) {
    snapTo((value + delta).coerceIn(0f, maxOffset))
}

/** 손을 뗀 자리가 통에 넣기로 볼 만큼 내려왔는지. */
private fun Animatable<Float, *>.reachedBin(dragDistancePx: Float): Boolean =
    dragDistancePx > 0f && value >= dragDistancePx * CARD_FOLD_DISCARD_THRESHOLD

private val DiscardSinkSpec = tween<Float>(CARD_FOLD_STEP_DURATION_MS, easing = FastOutSlowInEasing)

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
