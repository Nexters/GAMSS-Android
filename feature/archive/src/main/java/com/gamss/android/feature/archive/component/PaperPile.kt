package com.gamss.android.feature.archive.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.feature.archive.PaperBody
import com.gamss.android.feature.archive.PaperPhysicsWorld
import com.gamss.android.feature.archive.R
import com.gamss.android.feature.archive.designScale
import com.gamss.android.feature.archive.designWidth
import com.gamss.android.feature.archive.toDegrees
import com.gamss.android.feature.archive.toRadians
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
internal fun PaperPile(
    cards: List<CardEntry>,
    onPaperClick: (CardEntry) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = PileTopPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val scale = designScale(maxWidth)
        val density = LocalDensity.current
        val boundsWidthPx = with(density) { designWidth(scale).toPx() }
        val boundsHeightPx = with(density) { maxHeight.toPx() }
        val paperSizePx = with(density) { (PaperSize * scale).toPx() }
        val radiusPx = paperSizePx / 2f * PAPER_COLLISION_RADIUS_SCALE

        // 키에 화면 크기를 넣지 않는다. 회전 등으로 크기만 바뀌었을 때 이미 쌓인 종이가 다시 쏟아진다.
        val uiStates = remember(cards) { cards.spawnStates(boundsWidthPx, radiusPx) }

        LaunchedEffect(cards) {
            runPaperFall(uiStates, boundsWidthPx, boundsHeightPx, radiusPx)
        }

        cards.forEachIndexed { index, card ->
            val ui = uiStates.getOrNull(index) ?: return@forEachIndexed
            Image(
                painter = painterResource(R.drawable.archive_paper),
                contentDescription = stringResource(
                    R.string.archive_paper_description,
                    card.date.monthValue,
                    card.date.dayOfMonth,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(PaperSize * scale)
                    // 터치 영역도 그려진 자리를 따라가야 하므로 clickable 을 레이어 안쪽에 둔다.
                    .graphicsLayer {
                        translationX = ui.x - paperSizePx / 2f
                        translationY = ui.y - paperSizePx / 2f
                        rotationZ = ui.rotationDegrees
                    }
                    .clickable(role = Role.Button) { onPaperClick(card) },
            )
        }
    }
}

@Stable
private class PaperUiState(x: Float, y: Float, rotationDegrees: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var rotationDegrees by mutableFloatStateOf(rotationDegrees)
}

/** 화면 위쪽에 흩뿌린 시작 위치. 뒤 순번일수록 더 높이 두어 한꺼번에 떨어지지 않게 한다. */
private fun List<CardEntry>.spawnStates(boundsWidthPx: Float, radiusPx: Float): List<PaperUiState> =
    List(size) { index ->
        val spawnRange = (boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
        PaperUiState(
            x = radiusPx + Random.nextFloat() * spawnRange,
            y = -radiusPx - index * radiusPx * PAPER_SPAWN_STAGGER,
            rotationDegrees = (Random.nextFloat() - 0.5f) * 2f * PAPER_MAX_TILT_DEGREES,
        )
    }

private suspend fun CoroutineScope.runPaperFall(
    uiStates: List<PaperUiState>,
    boundsWidthPx: Float,
    boundsHeightPx: Float,
    radiusPx: Float,
) {
    val bodies = uiStates.map { ui ->
        PaperBody(
            startX = ui.x,
            startY = ui.y,
            startAngle = ui.rotationDegrees.toRadians(),
            radius = radiusPx,
            startVelX = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_DRIFT,
            startAngularVelocity = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_SPIN,
        )
    }
    val world = PaperPhysicsWorld(bodies, boundsWidth = boundsWidthPx, boundsHeight = boundsHeightPx)

    var lastFrameNanos = -1L
    var startFrameNanos = -1L
    var settledFrames = 0
    while (isActive) {
        withFrameNanos { frameNanos ->
            if (startFrameNanos < 0) startFrameNanos = frameNanos
            val dt = frameNanos.deltaSeconds(lastFrameNanos)
            lastFrameNanos = frameNanos

            world.step(dt)
            bodies.forEachIndexed { index, body ->
                uiStates[index].apply {
                    x = body.x
                    y = body.y
                    rotationDegrees = body.angle.toDegrees()
                }
            }
            settledFrames = if (world.maxActivity() < PHYSICS_SETTLE_THRESHOLD) settledFrames + 1 else 0
        }
        val settled = settledFrames >= PHYSICS_SETTLE_FRAMES
        val timedOut = lastFrameNanos - startFrameNanos > PHYSICS_MAX_DURATION_NANOS
        if (settled || timedOut) break
    }
}

/** 첫 프레임은 기준이 없어 한 프레임치로 두고, 프레임이 밀렸을 때는 위로 잘라 시뮬레이션이 튀지 않게 한다. */
private fun Long.deltaSeconds(previousNanos: Long): Float =
    if (previousNanos < 0) {
        PHYSICS_FIXED_DT
    } else {
        ((this - previousNanos) / NANOS_PER_SECOND).coerceIn(0f, PHYSICS_MAX_DT)
    }

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val PAPER_COLLISION_RADIUS_SCALE = 1.15f
private const val PAPER_MAX_TILT_DEGREES = 42f
private const val PAPER_SPAWN_STAGGER = 0.9f
private const val PAPER_SPAWN_DRIFT = 120f
private const val PAPER_SPAWN_SPIN = 0.15f
private const val PHYSICS_FIXED_DT = 1f / 60f
private const val PHYSICS_MAX_DT = 1f / 30f
private const val PHYSICS_SETTLE_THRESHOLD = 4f
private const val PHYSICS_SETTLE_FRAMES = 30
private const val PHYSICS_MAX_DURATION_NANOS = 5_000_000_000L

private val PileTopPadding = 136.dp
private val PaperSize = 88.dp
