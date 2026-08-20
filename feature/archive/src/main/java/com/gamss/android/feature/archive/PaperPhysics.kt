package com.gamss.android.feature.archive

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

internal fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)
internal fun Float.toDegrees(): Float = this * (180f / PI.toFloat())

internal class PaperBody(
    startX: Float,
    startY: Float,
    startAngle: Float,
    val radius: Float,
    startVelX: Float = 0f,
    startAngularVelocity: Float = 0f,
) {
    var x: Float = startX
    var y: Float = startY
    var velX: Float = startVelX
    var velY: Float = 0f
    var angle: Float = startAngle
    var angularVelocity: Float = startAngularVelocity

    /** 직전 step 을 시작할 때의 자리. 얼마나 움직였는지로 멈춤을 판단하는 데 쓴다. */
    var prevX: Float = startX
    var prevY: Float = startY
}

/**
 * 원 충돌 근사 기반의 경량 2D 리지드바디 시뮬레이터. 종이가 중력으로 떨어져 바닥·벽·서로에게 부딪혀
 * 쌓이는 연출용이다.
 *
 * 위치/속도를 불변 Vec2 가 아니라 [PaperBody] 의 Float 필드로 들고 계산도 Float 로 풀어 쓴다 —
 * 카드 20여 개면 프레임당 임시 객체가 수천 개씩 생겨 GC 를 압박한다.
 */
internal class PaperPhysicsWorld(
    private val bodies: List<PaperBody>,
    geometry: PaperGeometry,
) {
    /** 굴러다닐 칸. 화면 크기가 바뀌면 도는 중에도 갈아 끼운다. */
    var geometry: PaperGeometry = geometry

    /** 직전 [step] 의 간격. 옮긴 거리를 속도로 되돌리는 데 쓴다. */
    private var lastStepDt = 0f

    /**
     * 프레임을 기다리지 않고 잠잠해질 때까지 굴린다. 이미 쌓인 더미의 자리를 구하는 용도다.
     *
     * 중간에 취소를 확인한다. 화면을 벗어난 뒤에도 남은 step 을 다 도는 일이 없어야 한다.
     */
    suspend fun settle() {
        var settledSteps = 0
        repeat(PAPER_SETTLE_MAX_STEPS) {
            currentCoroutineContext().ensureActive()
            step(PAPER_FIXED_DT)
            settledSteps = if (isAtRest()) settledSteps + 1 else 0
            if (settledSteps >= PAPER_SETTLE_FRAMES) return
        }
    }

    fun step(dt: Float) {
        lastStepDt = dt
        bodies.forEach {
            it.prevX = it.x
            it.prevY = it.y
        }
        bodies.forEach { it.integrate(dt) }
        bodies.forEach(::resolveBounds)
        // 한 번만 풀면 여러 장이 겹친 자리에서 서로를 밀어내다 만다. 같은 계산을 몇 번 반복해 수렴시킨다.
        repeat(PAPER_COLLISION_ITERATIONS) { resolveAllPairs() }
    }

    /**
     * 직전 step 에서 어느 종이도 눈에 띄게 움직이지 않았는지. 낙하가 끝났는지 판단하는 쪽이 이
     * 값을 본다.
     *
     * 몸체의 속도로 재면 안 된다. 바닥에 놓인 종이도 매 step 중력만큼 속도를 얻었다가
     * [PAPER_RESTITUTION] 만큼만 되튕겨 흘려보내므로, 다 쌓인 뒤에도 속도는 0 근처로 내려가지 않는다.
     * 실제로 자리가 바뀌었는지를 봐야 멈춘 것을 알 수 있다.
     *
     * 옮긴 거리를 그대로 보지 않고 step 간격으로 나눈다. 프레임 간격은 기기 주사율에 따라 달라서,
     * 거리로 재면 120Hz 기기에서 아직 미끄러지는 중인 종이가 멈춘 것으로 잡힌다.
     */
    fun isAtRest(): Boolean {
        val maxMove = PAPER_REST_SPEED * lastStepDt
        val maxMoveSquared = maxMove * maxMove
        return bodies.all { body ->
            val dx = body.x - body.prevX
            val dy = body.y - body.prevY
            dx * dx + dy * dy < maxMoveSquared
        }
    }

    /** 좌우 벽과 바닥에 부딪히면 되튕기고, 스치는 방향으로 살짝 돌려 준다. */
    private fun resolveBounds(body: PaperBody) {
        val minX = body.radius
        val maxX = geometry.boundsWidthPx - body.radius
        val floorY = geometry.boundsHeightPx - body.radius

        when {
            body.x < minX -> {
                body.x = minX
                body.velX = -body.velX * PAPER_RESTITUTION
                body.angularVelocity -= body.velY * PAPER_WALL_SPIN_TRANSFER
            }

            body.x > maxX -> {
                body.x = maxX
                body.velX = -body.velX * PAPER_RESTITUTION
                body.angularVelocity += body.velY * PAPER_WALL_SPIN_TRANSFER
            }
        }

        if (body.y > floorY) {
            body.y = floorY
            body.velY = if (body.velY > 0f) -body.velY * PAPER_RESTITUTION else body.velY
            val friction = body.velX * PAPER_FRICTION
            body.velX = friction
            body.angularVelocity += friction * PAPER_FLOOR_SPIN_TRANSFER
        }
    }

    private fun resolveAllPairs() {
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                resolvePair(bodies[i], bodies[j])
            }
        }
    }
}

/** 중력과 감쇠를 먹여 한 프레임만큼 옮긴다. */
private fun PaperBody.integrate(dt: Float) {
    velY += PAPER_GRAVITY * dt
    x += velX * dt
    y += velY * dt
    angle += angularVelocity * dt
    velX *= PAPER_LINEAR_DAMPING
    velY *= PAPER_LINEAR_DAMPING

    val damped = (angularVelocity * PAPER_ANGULAR_DAMPING)
        .coerceIn(-PAPER_MAX_ANGULAR_VELOCITY, PAPER_MAX_ANGULAR_VELOCITY)
    // 0 에 가까운 회전은 0 으로 끊는다. 안 그러면 쌓인 종이끼리 매 프레임 미세한 토크를 주고받아
    // 제자리에서 계속 돈다.
    angularVelocity = if (abs(damped) < PAPER_ANGULAR_SLEEP_THRESHOLD) 0f else damped
}

/** 겹친 두 장을 떼어 놓고, 부딪힌 세기만큼 되튕기며 스친 만큼 돌려 준다. */
private fun resolvePair(a: PaperBody, b: PaperBody) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val distance = sqrt(dx * dx + dy * dy)
    val minDistance = a.radius + b.radius
    // 아직 안 닿았거나, 정확히 겹쳐 어느 쪽으로 밀어낼지 정할 수 없는 경우다.
    if (distance >= minDistance || distance <= PAPER_MIN_SEPARATION_DISTANCE) return

    val normalX = dx / distance
    val normalY = dy / distance
    pushApart(a, b, normalX, normalY, gap = minDistance - distance)

    // 되튕김과 회전은 둘 다 부딪히기 직전의 상대 속도로 계산한다.
    val relVelX = b.velX - a.velX
    val relVelY = b.velY - a.velY
    val approachSpeed = relVelX * normalX + relVelY * normalY
    // 겹쳐 있어도 이미 서로 멀어지는 중이면 더 손댈 게 없다.
    if (approachSpeed > 0f) return

    applyBounce(a, b, normalX, normalY, approachSpeed)
    applySlidingSpin(a, b, tangentSpeed = relVelY * normalX - relVelX * normalY)
}

/** 겹친 만큼 서로 반씩 밀어낸다. [PAPER_POSITION_SLOP] 만큼은 남겨 둬야 다음 프레임에 다시 밀리며 떨지 않는다. */
private fun pushApart(a: PaperBody, b: PaperBody, normalX: Float, normalY: Float, gap: Float) {
    val halfOverlap = (gap - PAPER_POSITION_SLOP).coerceAtLeast(0f) * PAPER_EQUAL_MASS_SHARE
    val correctionX = normalX * halfOverlap
    val correctionY = normalY * halfOverlap
    a.x -= correctionX
    a.y -= correctionY
    b.x += correctionX
    b.y += correctionY
}

/** 법선 방향으로 되튕긴다. 종이 무게는 모두 같다고 보고 충격량을 반씩 나눈다. */
private fun applyBounce(
    a: PaperBody,
    b: PaperBody,
    normalX: Float,
    normalY: Float,
    approachSpeed: Float,
) {
    val magnitude = -(1f + PAPER_RESTITUTION) * approachSpeed * PAPER_EQUAL_MASS_SHARE
    val impulseX = normalX * magnitude
    val impulseY = normalY * magnitude
    a.velX -= impulseX
    a.velY -= impulseY
    b.velX += impulseX
    b.velY += impulseY
}

/**
 * 접선 방향으로 스친 만큼 두 장을 반대로 돌린다.
 *
 * 다 쌓인 뒤에도 중력으로 매 프레임 서로 닿으며 미세한 접선 속도가 생긴다. 그 이하는 무시한다.
 */
private fun applySlidingSpin(a: PaperBody, b: PaperBody, tangentSpeed: Float) {
    if (abs(tangentSpeed) <= PAPER_RESTING_TANGENT_SPEED) return

    val spinImpulse = tangentSpeed * PAPER_PAIR_SPIN_TRANSFER
    a.angularVelocity -= spinImpulse
    b.angularVelocity += spinImpulse
}
