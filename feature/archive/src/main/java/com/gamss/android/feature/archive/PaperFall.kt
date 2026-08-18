package com.gamss.android.feature.archive

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * 종이 [count] 장을 화면 위쪽에 흩뿌려 두고, [run] 이 도는 동안 [papers] 의 자리를 프레임마다 갱신한다.
 * 떨어지고 부딪히는 계산 자체는 [PaperPhysicsWorld] 가 맡는다.
 */
@Stable
internal class PaperFall(
    count: Int,
    private val boundsWidthPx: Float,
    private val boundsHeightPx: Float,
    private val radiusPx: Float,
) {
    /** 그리는 쪽이 읽어 가는 현재 자리. 프레임마다 바뀐다. */
    val papers: List<PaperUiState> = spawnPapers(count, boundsWidthPx, radiusPx)

    /**
     * 다 쌓여 잠잠해지거나 [PHYSICS_MAX_DURATION_NANOS] 가 지나면 돌아온다 — 화면이 그대로인데도
     * 매 프레임 계속 깨어나지 않게 한다.
     */
    suspend fun run() = coroutineScope {
        val bodies = papers.map { it.toBody(radiusPx) }
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
                bodies.forEachIndexed { index, body -> papers[index].follow(body) }
                settledFrames = if (world.maxActivity() < PHYSICS_SETTLE_THRESHOLD) settledFrames + 1 else 0
            }
            val settled = settledFrames >= PHYSICS_SETTLE_FRAMES
            val timedOut = lastFrameNanos - startFrameNanos > PHYSICS_MAX_DURATION_NANOS
            if (settled || timedOut) break
        }
    }
}

/** 종이 한 장이 지금 놓인 자리. 프레임마다 바뀌므로 Compose 상태로 든다. */
@Stable
internal class PaperUiState(x: Float, y: Float, rotationDegrees: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var rotationDegrees by mutableFloatStateOf(rotationDegrees)
}

/** 가로로 흩뿌린 시작 위치. 뒤 순번일수록 더 높이 두어 한꺼번에 떨어지지 않게 한다. */
private fun spawnPapers(count: Int, boundsWidthPx: Float, radiusPx: Float): List<PaperUiState> {
    val spawnRange = (boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
    return List(count) { index ->
        PaperUiState(
            x = radiusPx + Random.nextFloat() * spawnRange,
            y = -radiusPx - index * radiusPx * PAPER_SPAWN_STAGGER,
            rotationDegrees = (Random.nextFloat() - 0.5f) * 2f * PAPER_MAX_TILT_DEGREES,
        )
    }
}

/** 흩뿌린 자리에서 그대로 출발한다. 낙하가 너무 반듯해 보이지 않게 옆으로 밀고 살짝 돌려 둔다. */
private fun PaperUiState.toBody(radiusPx: Float) = PaperBody(
    startX = x,
    startY = y,
    startAngle = rotationDegrees.toRadians(),
    radius = radiusPx,
    startVelX = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_DRIFT,
    startAngularVelocity = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_SPIN,
)

private fun PaperUiState.follow(body: PaperBody) {
    x = body.x
    y = body.y
    rotationDegrees = body.angle.toDegrees()
}

/** 첫 프레임은 기준이 없어 한 프레임치로 두고, 프레임이 밀렸을 때는 위로 잘라 시뮬레이션이 튀지 않게 한다. */
private fun Long.deltaSeconds(previousNanos: Long): Float =
    if (previousNanos < 0) {
        PHYSICS_FIXED_DT
    } else {
        ((this - previousNanos) / NANOS_PER_SECOND).coerceIn(0f, PHYSICS_MAX_DT)
    }

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val PAPER_MAX_TILT_DEGREES = 42f
private const val PAPER_SPAWN_STAGGER = 0.9f
private const val PAPER_SPAWN_DRIFT = 120f
private const val PAPER_SPAWN_SPIN = 0.15f
private const val PHYSICS_FIXED_DT = 1f / 60f
private const val PHYSICS_MAX_DT = 1f / 30f
private const val PHYSICS_SETTLE_THRESHOLD = 4f
private const val PHYSICS_SETTLE_FRAMES = 30
private const val PHYSICS_MAX_DURATION_NANOS = 5_000_000_000L
