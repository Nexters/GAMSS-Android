package com.gamss.android.feature.archive

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** 종이가 굴러다닐 수 있는 칸과 종이 한 장의 충돌 반지름. */
internal data class PaperGeometry(
    val boundsWidthPx: Float,
    val boundsHeightPx: Float,
    val radiusPx: Float,
)

/**
 * 종이 [count] 장을 화면 위쪽에 흩뿌려 두고, [run] 이 도는 동안 [papers] 의 자리를 프레임마다 갱신한다.
 * 떨어지고 부딪히는 계산 자체는 [PaperPhysicsWorld] 가 맡는다.
 *
 * 칸의 크기는 생성 시점에 박지 않고 [geometry] 로 들고 프레임마다 다시 읽는다. 접는 기기를 펴거나
 * 화면을 나눠 쓰면 칸이 바뀌는데, 처음 크기를 그대로 쓰면 종이가 옛 바닥과 벽을 기준으로 계속 굴러
 * 화면 밖에 쌓인다.
 *
 * @param droppingIndex 이 자리의 종이 한 장만 떨어뜨리고 나머지는 이미 쌓인 자리에서 시작한다.
 *  방금 버린 카드로 들어왔을 때 쓴다. null 이면 전부 위에서 쏟는다.
 */
@Stable
internal class PaperFall(
    count: Int,
    geometry: PaperGeometry,
    private val droppingIndex: Int?,
) {
    var geometry: PaperGeometry = geometry
        private set

    /** 그리는 쪽이 읽어 가는 현재 자리. 프레임마다 바뀐다. */
    val papers: List<PaperUiState> = spawnPapers(count, geometry, droppingIndex)

    fun updateGeometry(geometry: PaperGeometry) {
        this.geometry = geometry
    }

    /**
     * 다 쌓여 잠잠해지거나 [PAPER_MAX_DURATION_NANOS] 가 지나면 돌아온다 — 화면이 그대로인데도
     * 매 프레임 계속 깨어나지 않게 한다.
     */
    suspend fun run() = coroutineScope {
        val bodies = papers.map { it.toBody(geometry.radiusPx) }
        settleAlreadyPiled(bodies)
        val world = PaperPhysicsWorld(bodies, geometry)

        var lastFrameNanos = -1L
        var startFrameNanos = -1L
        var settledFrames = 0
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (startFrameNanos < 0) startFrameNanos = frameNanos
                val dt = frameNanos.deltaSeconds(lastFrameNanos)
                lastFrameNanos = frameNanos

                world.geometry = geometry
                world.step(dt)
                bodies.forEachIndexed { index, body -> papers[index].follow(body) }
                settledFrames = if (world.isAtRest()) settledFrames + 1 else 0
            }
            val settled = settledFrames >= PAPER_SETTLE_FRAMES
            val timedOut = lastFrameNanos - startFrameNanos > PAPER_MAX_DURATION_NANOS
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
            PaperPhysicsWorld(piled, geometry).settle()
        }
        bodies.forEachIndexed { index, body -> if (index != droppingIndex) papers[index].follow(body) }
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
    geometry: PaperGeometry,
    droppingIndex: Int?,
): List<PaperUiState> {
    val radiusPx = geometry.radiusPx
    val spawnRange = (geometry.boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
    return List(count) { index ->
        val stackHeight = if (index == droppingIndex) 0f else index * radiusPx * PAPER_SPAWN_STAGGER
        PaperUiState(
            x = radiusPx + Random.nextFloat() * spawnRange,
            y = -radiusPx - stackHeight,
            rotationDegrees = Random.symmetric(PAPER_MAX_TILT_DEGREES),
        )
    }
}

/** 흩뿌린 자리에서 그대로 출발한다. 낙하가 너무 반듯해 보이지 않게 옆으로 밀고 살짝 돌려 둔다. */
private fun PaperUiState.toBody(radiusPx: Float) = PaperBody(
    startX = x,
    startY = y,
    startAngle = rotationDegrees.toRadians(),
    radius = radiusPx,
    startVelX = Random.symmetric(PAPER_SPAWN_DRIFT),
    startAngularVelocity = Random.symmetric(PAPER_SPAWN_SPIN),
)

private fun PaperUiState.follow(body: PaperBody) {
    x = body.x
    y = body.y
    rotationDegrees = body.angle.toDegrees()
}

private fun Random.symmetric(range: Float): Float = (nextFloat() - 0.5f) * 2f * range

/** 첫 프레임은 기준이 없어 한 프레임치로 두고, 프레임이 밀렸을 때는 위로 잘라 시뮬레이션이 튀지 않게 한다. */
private fun Long.deltaSeconds(previousNanos: Long): Float =
    if (previousNanos < 0) {
        PAPER_FIXED_DT
    } else {
        ((this - previousNanos) / NANOS_PER_SECOND).coerceIn(0f, PAPER_MAX_DT)
    }

private const val NANOS_PER_SECOND = 1_000_000_000f
