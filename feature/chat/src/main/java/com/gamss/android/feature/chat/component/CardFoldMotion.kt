package com.gamss.android.feature.chat.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.designScale
import com.gamss.android.feature.chat.CARD_FOLD_ARROW_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_BIN_ASPECT_RATIO
import com.gamss.android.feature.chat.CARD_FOLD_BIN_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_BIN_SHOWN_MS
import com.gamss.android.feature.chat.CARD_FOLD_BIN_SINK_FRACTION
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_HOLD_MS
import com.gamss.android.feature.chat.CARD_FOLD_DISCARD_THRESHOLD
import com.gamss.android.feature.chat.CARD_FOLD_GUIDE_DELAY_MS
import com.gamss.android.feature.chat.CARD_FOLD_HINT_FADE_MS
import com.gamss.android.feature.chat.CardFoldReturnSpec
import com.gamss.android.feature.chat.CardFoldSinkSpec
import com.gamss.android.feature.chat.CardFoldStage
import com.gamss.android.feature.chat.paperSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 통은 창 아래에 붙고 종이는 창 중심에 붙으므로, 내려갈 거리가 화면 높이 상수가 아니라 이 창의
 * 크기에서 바로 나온다. 그래서 인셋이나 폰트 배율이 바뀌어도 어긋나지 않는다.
 */
@Stable
internal data class CardFoldMetrics(
    val scale: Float,
    val paperSize: DpSize,
    val binHeight: Dp,
    val travel: DiscardTravel,
)

@Composable
internal fun rememberCardFoldMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
    foldStage: CardFoldStage,
): CardFoldMetrics {
    val density = LocalDensity.current
    return remember(maxWidth, maxHeight, foldStage, density) {
        // 시안은 402dp 폭 기준이다. 좁은 기기에서 종이와 힌트를 폭 비율만큼 함께 줄여 좌우 여백
        // 비율을 지킨다. 통은 폭을 채우고 높이를 비율로 뽑으므로 이미 같은 비율로 줄어든다.
        val scale = designScale(maxWidth)
        val paperSize = foldStage.paperSize * scale
        val binHeight = maxWidth / CARD_FOLD_BIN_ASPECT_RATIO
        CardFoldMetrics(
            scale = scale,
            paperSize = paperSize,
            binHeight = binHeight,
            travel = with(density) { discardTravel(maxHeight, binHeight, paperSize.height) },
        )
    }
}

/** 셋 다 버리는 동안에도 남아야 하므로, 끌 수 있는지가 아니라 접힘 단계로 판단한다. */
@Stable
internal class CardFoldHints(
    val bin: State<Float>,
    val guide: State<Float>,
    val arrow: State<Float>,
)

@Composable
internal fun rememberCardFoldHints(folded: Boolean): CardFoldHints {
    val bin = fadeInAfter(folded, CARD_FOLD_BIN_DELAY_MS, label = "cardFoldBin")
    val guide = fadeInAfter(folded, CARD_FOLD_GUIDE_DELAY_MS, label = "cardFoldGuide")
    val arrow = fadeInAfter(folded, CARD_FOLD_ARROW_DELAY_MS, label = "cardFoldArrow")
    return remember(bin, guide, arrow) { CardFoldHints(bin, guide, arrow) }
}

@Composable
private fun fadeInAfter(visible: Boolean, delayMillis: Int, label: String): State<Float> =
    animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(CARD_FOLD_HINT_FADE_MS, delayMillis = delayMillis),
        label = label,
    )

/**
 * 끄는 동안은 [offset] 을 직접 바꾼다. Animatable 을 두고 델타마다 코루틴을 열어 snapTo 를 부르면,
 * 손을 뗀 뒤 시작한 가라앉기 애니메이션을 뒤늦게 도착한 snapTo 가 Animatable 의 MutatorMutex 로
 * 취소한다(같은 우선순위는 나중 것이 이긴다). 그러면 종이만 멈춰 선 채 [onDiscard] 가 영영 안
 * 불려, 나갈 길이 없는 화면이 남는다.
 *
 * 가라앉기는 제스처 콜백이 아니라 컴포지션 스코프에서 돌린다. 버리기가 확정된 뒤의 연출은
 * 제스처보다 오래 살아야 한다.
 */
@Stable
internal class CardFoldDragState(private val scope: CoroutineScope) {

    val offset = mutableFloatStateOf(0f)

    var discarding by mutableStateOf(false)
        private set

    internal var travel by mutableStateOf(DiscardTravel.None)
        private set

    internal var onDiscard: () -> Unit = {}

    val draggableState = DraggableState { delta ->
        offset.floatValue = (offset.floatValue + delta).coerceIn(0f, travel.dragDistancePx)
    }

    /** 1 이면 종이 아래끝이 통 입구에 닿았다. */
    fun dragFraction(): Float {
        val distance = travel.dragDistancePx
        return if (distance > 0f) (offset.floatValue / distance).coerceIn(0f, 1f) else 0f
    }

    /**
     * 창 크기가 바뀌면 내려갈 거리도 바뀐다. 이미 끌어 둔 자리가 새 거리를 넘으면 종이가 통
     * 아래로 튀어나오므로 다시 안으로 넣는다. 가라앉는 중에는 손대지 않는다. 그때의 [offset] 은
     * 일부러 [DiscardTravel.dragDistancePx] 를 넘어 통 뒤까지 가 있다.
     */
    fun onTravelChanged(travel: DiscardTravel) {
        if (this.travel == travel) return
        this.travel = travel
        if (!discarding) {
            offset.floatValue = offset.floatValue.coerceIn(0f, travel.dragDistancePx)
        }
    }

    fun onDragStopped() {
        if (reachedBin()) {
            discarding = true
            scope.launch {
                animate(
                    initialValue = offset.floatValue,
                    targetValue = travel.swallowDistancePx,
                    animationSpec = CardFoldSinkSpec,
                ) { value, _ -> offset.floatValue = value }
                delay(CARD_FOLD_DISCARD_HOLD_MS)
                onDiscard()
            }
        } else {
            scope.launch {
                animate(offset.floatValue, 0f, animationSpec = CardFoldReturnSpec) { value, _ ->
                    offset.floatValue = value
                }
            }
        }
    }

    private fun reachedBin(): Boolean {
        val distance = travel.dragDistancePx
        return distance > 0f && offset.floatValue >= distance * CARD_FOLD_DISCARD_THRESHOLD
    }
}

@Composable
internal fun rememberCardFoldDragState(
    travel: DiscardTravel,
    onDiscard: () -> Unit,
): CardFoldDragState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { CardFoldDragState(scope) }
    SideEffect {
        state.onDiscard = onDiscard
        state.onTravelChanged(travel)
    }
    return state
}

/**
 * 종이가 통까지 움직일 거리.
 *
 * @property dragDistancePx 손으로 끌 수 있는 끝. 종이 아래끝이 입구에 닿는 지점이다. 더 끌리게
 *  두면 종이가 통 뒤로 숨어 어디까지 왔는지 안 보인다.
 * @property swallowDistancePx 손을 뗀 뒤 가라앉는 끝. 종이 위끝까지 입구 아래로 넣어 통 뒤로
 *  완전히 감춘다.
 */
internal data class DiscardTravel(val dragDistancePx: Float, val swallowDistancePx: Float) {
    companion object {
        /** 아직 창 크기를 못 잰 상태. */
        val None = DiscardTravel(dragDistancePx = 0f, swallowDistancePx = 0f)
    }
}

private fun Density.discardTravel(windowHeight: Dp, binHeight: Dp, paperHeight: Dp): DiscardTravel {
    val sinkTarget = windowHeight - binHeight * (1f - CARD_FOLD_BIN_SINK_FRACTION)
    return DiscardTravel(
        dragDistancePx = (sinkTarget - (windowHeight + paperHeight) / 2).coerceAtLeast(0.dp).toPx(),
        swallowDistancePx = (sinkTarget - (windowHeight - paperHeight) / 2).coerceAtLeast(0.dp).toPx(),
    )
}

/**
 * 힌트 진행률을 컴포지션에서 읽지 않고 [CARD_FOLD_BIN_SHOWN_MS] 로 다시 잰다. 진행률을 여기서
 * 읽으면 통이 배어 나오는 동안 매 프레임 카드까지 다시 그려진다.
 */
@Composable
internal fun rememberBinReady(folded: Boolean): Boolean {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(folded) {
        ready = false
        if (folded) {
            delay(CARD_FOLD_BIN_SHOWN_MS.toLong())
            ready = true
        }
    }
    return ready
}
