package com.gamss.android.feature.carddelete.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.feature.carddelete.SHRED_TAP_STEP_DURATION_MS
import com.gamss.android.feature.carddelete.SHRED_TEAR_FRACTION
import com.gamss.android.core.designsystem.R as DesignSystemR

@Composable
fun ShredStrips(progress: Float, modifier: Modifier = Modifier) {
    val displayProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        if (progress < displayProgress.value) {
            displayProgress.snapTo(progress)
        } else {
            displayProgress.animateTo(
                progress,
                tween(SHRED_TAP_STEP_DURATION_MS, easing = FastOutSlowInEasing),
            )
        }
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val stripMaxHeight = maxHeight * STRIP_MAX_HEIGHT_FRACTION
        val fallDistance = maxHeight + stripMaxHeight
        val tearProgress = (displayProgress.value / SHRED_TEAR_FRACTION).coerceIn(0f, 1f)
        val fallProgress = ((displayProgress.value - SHRED_TEAR_FRACTION) / (1f - SHRED_TEAR_FRACTION))
            .coerceIn(0f, 1f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = StripRowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(StripGap),
        ) {
            LaneSpecs.forEach { lane ->
                ShredStrip(
                    modifier = Modifier.weight(1f),
                    heightFraction = tearProgress,
                    fallFraction = (
                        (fallProgress - lane.fallStartOffset) / (1f - lane.fallStartOffset)
                        ).coerceIn(0f, 1f),
                    fallDistance = fallDistance,
                    targetHeight = stripMaxHeight * lane.targetHeightRatio,
                )
            }
        }
    }
}

@Composable
private fun ShredStrip(
    heightFraction: Float,
    fallFraction: Float,
    fallDistance: Dp,
    targetHeight: Dp,
    modifier: Modifier,
) {
    val currentHeight = StripBaseHeight + (targetHeight - StripBaseHeight) * heightFraction
    Image(
        painter = painterResource(DesignSystemR.drawable.img_paper_strip),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .height(currentHeight)
            .graphicsLayer { translationY = fallDistance.toPx() * fallFraction },
    )
}

private data class LaneSpec(val targetHeightRatio: Float, val fallStartOffset: Float)

private val LaneSpecs = listOf(
    LaneSpec(targetHeightRatio = 0.55f, fallStartOffset = 0.00f),
    LaneSpec(targetHeightRatio = 0.71f, fallStartOffset = 0.05f),
    LaneSpec(targetHeightRatio = 0.48f, fallStartOffset = 0.02f),
    LaneSpec(targetHeightRatio = 0.90f, fallStartOffset = 0.08f),
    LaneSpec(targetHeightRatio = 0.71f, fallStartOffset = 0.01f),
    LaneSpec(targetHeightRatio = 0.55f, fallStartOffset = 0.06f),
    LaneSpec(targetHeightRatio = 1.00f, fallStartOffset = 0.00f),
    LaneSpec(targetHeightRatio = 0.80f, fallStartOffset = 0.07f),
)
private val StripBaseHeight = 28.dp
private const val STRIP_MAX_HEIGHT_FRACTION = 0.55f
private val StripGap = 10.dp
private val StripRowHorizontalPadding = 24.dp
