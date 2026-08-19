package com.gamss.android.feature.chat.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.gamss.android.feature.chat.CARD_FOLD_ARROW_ALPHA
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_HOLD_MS
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_THRESHOLD
import com.gamss.android.feature.chat.CARD_FOLD_STEP_DURATION_MS
import com.gamss.android.feature.chat.CardFoldArrowSize
import com.gamss.android.feature.chat.CardFoldBinHeight
import com.gamss.android.feature.chat.CardFoldBinMouthHeight
import com.gamss.android.feature.chat.CardFoldBinSinkDepth
import com.gamss.android.feature.chat.CardFoldGuideGap
import com.gamss.android.feature.chat.CardFoldStage
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
 * 만들어져 되돌릴 수 없으므로 [Dialog] 의 취소 경로(뒤로가기·바깥 탭)를 모두 닫아 둔다.
 */
@Composable
internal fun CardFoldOverlay(
    card: Card,
    foldStage: CardFoldStage,
    onFoldTap: () -> Unit,
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
            onDiscard = onDiscard,
        )
    }
}

@Composable
private fun CardFoldContent(
    card: Card,
    foldStage: CardFoldStage,
    onFoldTap: () -> Unit,
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
    // 통까지 남은 거리는 실제로 배치된 좌표에서 뽑는다. 화면 높이와 여백으로 되짚으면
    // 인셋이나 폰트 배율이 바뀔 때마다 어긋난다.
    var paperRestBottomPx by remember { mutableFloatStateOf(0f) }
    var binMouthTopPx by remember { mutableFloatStateOf(0f) }
    val sinkDepthPx = with(LocalDensity.current) { CardFoldBinSinkDepth.toPx() }
    val dragDistancePx = (binMouthTopPx + sinkDepthPx - paperRestBottomPx).coerceAtLeast(0f)

    // 진행률은 graphicsLayer 안에서만 불러 쓴다. 컴포지션 본문에서 dragOffset.value 를 읽으면
    // 끄는 동안 매 프레임 카드까지 다시 그려진다.
    val dragFraction = { if (dragDistancePx > 0f) (dragOffset.value / dragDistancePx).coerceIn(0f, 1f) else 0f }

    val canFold = foldStage.next != null && !isDiscarding
    val canDrag = foldStage.next == null && !isDiscarding

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val guideRes = if (canDrag) {
                R.string.chat_room_card_fold_discard_guide
            } else {
                R.string.chat_room_card_fold_guide
            }
            Text(
                text = stringResource(guideRes),
                style = GamssTheme.typography.body3Medium,
                color = GamssTheme.colors.white,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = 1f - dragFraction() },
            )
            Spacer(modifier = Modifier.height(CardFoldGuideGap))
            // 제자리 좌표는 이 Box 가 잰다. 안쪽 graphicsLayer 의 이동량까지 섞이면 끌수록
            // 통까지 남은 거리가 함께 줄어 판정이 어긋난다.
            Box(modifier = Modifier.onGloballyPositioned { paperRestBottomPx = it.boundsInRoot().bottom }) {
                FoldingPaper(
                    card = card,
                    foldProgress = foldProgress.value,
                    modifier = Modifier
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
            if (canDrag) {
                DiscardArrow(alpha = dragFraction)
            }
        }

        // 종이보다 뒤에 그리면 통 앞면에 가려 파묻히는 모습이 안 나온다.
        DiscardBin(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { binMouthTopPx = it.boundsInRoot().top },
        )
    }
}

/**
 * 세 단계 그림을 겹쳐 두고 [foldProgress] 에 가까운 것만 보여 준다. 각 그림은 자기 원본 크기로
 * 배치한 뒤 보간된 크기까지 눌러서, 카드가 접히며 찌그러지는 모습이 그대로 나온다.
 *
 * 접히는 기준선은 위쪽이다. Figma 에서 세 단계가 모두 같은 y 에서 시작한다.
 */
@Composable
private fun FoldingPaper(
    card: Card,
    foldProgress: Float,
    modifier: Modifier = Modifier,
) {
    val size = lerpPaperSize(foldProgress)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.TopCenter,
    ) {
        CardFoldStage.entries.forEach { stage ->
            val alpha = (1f - abs(foldProgress - stage.ordinal)).coerceIn(0f, 1f)
            if (alpha == 0f) return@forEach

            val natural = stage.paperSize
            Box(
                modifier = Modifier
                    .size(natural)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = size.width / natural.width
                        scaleY = size.height / natural.height
                        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                    },
            ) {
                StagePaper(stage = stage, card = card)
            }
        }
    }
}

@Composable
private fun StagePaper(stage: CardFoldStage, card: Card) {
    when (stage) {
        CardFoldStage.Unfolded -> GamssImageCard(
            date = card.date.format(CardFoldDateFormatter),
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
 * 아래로 끌어내리라는 힌트.
 *
 * Figma trash-04 의 주황 화살표 에셋이 아직 없어 기존 아이콘으로 대신한다. 에셋을 받으면
 * 이 컴포저블의 painter 만 갈아 끼운다.
 */
@Composable
private fun DiscardArrow(alpha: () -> Float) {
    Icon(
        painter = painterResource(GamssIcons.ScrollDown),
        contentDescription = null,
        tint = GamssTheme.colors.red,
        modifier = Modifier
            .size(CardFoldArrowSize)
            .graphicsLayer { this.alpha = (1f - alpha()) * CARD_FOLD_ARROW_ALPHA },
    )
}

/**
 * 화면 아래에 걸친 쓰레기통. 입구 띠와 몸통으로만 그린다.
 *
 * Figma trash-04 의 통 에셋이 아직 없어 도형으로 대신한다. 에셋을 받으면 이 컴포저블 안의
 * 두 [Box] 를 이미지 하나로 갈아 끼운다.
 */
@Composable
private fun DiscardBin(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardFoldBinMouthHeight)
                .clip(RoundedCornerShape(topStart = BinCornerRadius, topEnd = BinCornerRadius))
                .background(GamssTheme.colors.gray400),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardFoldBinHeight - CardFoldBinMouthHeight)
                .background(GamssTheme.colors.gray500),
        )
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

private val BinCornerRadius = 12.dp
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
