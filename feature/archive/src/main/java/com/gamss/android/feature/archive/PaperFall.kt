package com.gamss.android.feature.archive

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 종이 [count] 장을 화면 위쪽에 흩뿌려 두고, [run] 이 도는 동안 [papers] 의 자리를 프레임마다 갱신한다.
 * 떨어지고 부딪히는 계산 자체는 [PaperPhysicsWorld] 가 맡는다.
 *
 * @param droppingIndex 이 자리의 종이 한 장만 떨어뜨리고 나머지는 이미 쌓인 자리에서 시작한다.
 *  방금 버린 카드로 들어왔을 때 쓴다. null 이면 전부 위에서 쏟는다.
 */
@Stable
internal class PaperFall(
    count: Int,
    private val boundsWidthPx: Float,
    private val boundsHeightPx: Float,
    private val radiusPx: Float,
    private val droppingIndex: Int? = null,
) {
    /** 그리는 쪽이 읽어 가는 현재 자리. 프레임마다 바뀐다. */
    val papers: List<PaperUiState> = spawnPapers(count, boundsWidthPx, radiusPx, droppingIndex)

    /**
     * 다 쌓여 잠잠해지거나 [PHYSICS_MAX_DURATION_NANOS] 가 지나면 돌아온다 — 화면이 그대로인데도
     * 매 프레임 계속 깨어나지 않게 한다.
     */
    suspend fun run() = coroutineScope {
        val bodies = papers.map { it.toBody(radiusPx) }
        settleAlreadyPiled(bodies)
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
                settledFrames = if (world.isAtRest()) settledFrames + 1 else 0
            }
            val settled = settledFrames >= PHYSICS_SETTLE_FRAMES
            val timedOut = lastFrameNanos - startFrameNanos > PHYSICS_MAX_DURATION_NANOS
            if (settled || timedOut) break
        }
    }

    /**
     * 이미 쌓여 있던 종이를 화면에 나오기 전에 바닥까지 굴려 둔다.
     *
     * 떨어질 한 장은 이 계산에서 빼야 한다. 같이 굴리면 그 장이 먼저 내려가 자리를 차지하고,
     * 정작 보여 줄 낙하가 남지 않는다.
     */
    private suspend fun settleAlreadyPiled(bodies: List<PaperBody>) {
        val droppingIndex = droppingIndex ?: return
        val piled = bodies.filterIndexed { index, _ -> index != droppingIndex }
        if (piled.isEmpty()) return

        // 프레임 밖에서 굴린다. 종이가 많으면 한 번에 수십만 번의 충돌 계산이라, 메인 스레드에
        // 두면 화면이 밀려 들어오는 트랜지션 위에 그대로 얹힌다.
        withContext(Dispatchers.Default) {
            PaperPhysicsWorld(piled, boundsWidth = boundsWidthPx, boundsHeight = boundsHeightPx).settle()
        }
        bodies.forEachIndexed { index, body -> if (index != droppingIndex) papers[index].follow(body) }
    }
}

/**
 * 프레임을 기다리지 않고 잠잠해질 때까지 굴린다. 이미 쌓인 더미의 자리를 구하는 용도다.
 *
 * 중간에 취소를 확인한다. 화면을 벗어난 뒤에도 남은 step 을 다 도는 일이 없어야 한다.
 */
private suspend fun PaperPhysicsWorld.settle() {
    var settledSteps = 0
    repeat(PHYSICS_SETTLE_MAX_STEPS) {
        currentCoroutineContext().ensureActive()
        step(PHYSICS_FIXED_DT)
        settledSteps = if (isAtRest()) settledSteps + 1 else 0
        if (settledSteps >= PHYSICS_SETTLE_FRAMES) return
    }
}

/** 종이 한 장이 지금 놓인 자리. 프레임마다 바뀌므로 Compose 상태로 든다. */
@Stable
internal class PaperUiState(x: Float, y: Float, rotationDegrees: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var rotationDegrees by mutableFloatStateOf(rotationDegrees)
}

/**
 * 가로로 흩뿌린 시작 위치. 뒤 순번일수록 더 높이 두어 한꺼번에 떨어지지 않게 한다.
 *
 * 한 장만 떨어질 때 그 장은 화면 바로 위에 둔다. 순번대로 높이를 주면 뒤쪽 카드일수록 한참
 * 뒤에야 화면에 들어온다.
 */
private fun spawnPapers(
    count: Int,
    boundsWidthPx: Float,
    radiusPx: Float,
    droppingIndex: Int?,
): List<PaperUiState> {
    val spawnRange = (boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
    return List(count) { index ->
        val stackHeight = if (index == droppingIndex) 0f else index * radiusPx * PAPER_SPAWN_STAGGER
        PaperUiState(
            x = radiusPx + Random.nextFloat() * spawnRange,
            y = -radiusPx - stackHeight,
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
private const val PHYSICS_SETTLE_FRAMES = 30
private const val PHYSICS_MAX_DURATION_NANOS = 5_000_000_000L

/** 미리 굴릴 때의 상한. 10초치라 실제로는 훨씬 먼저 잠잠해진다. */
private const val PHYSICS_SETTLE_MAX_STEPS = 600
