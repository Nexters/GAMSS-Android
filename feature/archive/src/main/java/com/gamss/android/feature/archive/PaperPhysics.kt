package com.gamss.android.feature.archive

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
    private val boundsWidth: Float,
    private val boundsHeight: Float,
) {
    fun step(dt: Float) {
        bodies.forEach { it.integrate(dt) }
        bodies.forEach(::resolveBounds)
        // 한 번만 풀면 여러 장이 겹친 자리에서 서로를 밀어내다 만다. 같은 계산을 몇 번 반복해 수렴시킨다.
        repeat(COLLISION_ITERATIONS) { resolveAllPairs() }
    }

    /** 가장 활발한 종이가 얼마나 움직이는지. 낙하가 끝났는지 판단하는 쪽이 이 값을 본다. */
    fun maxActivity(): Float = bodies.maxOf { body ->
        val linear = sqrt(body.velX * body.velX + body.velY * body.velY)
        val angular = abs(body.angularVelocity) * ANGULAR_ACTIVITY_WEIGHT
        linear + angular
    }

    /** 좌우 벽과 바닥에 부딪히면 되튕기고, 스치는 방향으로 살짝 돌려 준다. */
    private fun resolveBounds(body: PaperBody) {
        val minX = body.radius
        val maxX = boundsWidth - body.radius
        val floorY = boundsHeight - body.radius

        when {
            body.x < minX -> {
                body.x = minX
                body.velX = -body.velX * RESTITUTION
                body.angularVelocity -= body.velY * WALL_SPIN_TRANSFER
            }

            body.x > maxX -> {
                body.x = maxX
                body.velX = -body.velX * RESTITUTION
                body.angularVelocity += body.velY * WALL_SPIN_TRANSFER
            }
        }

        if (body.y > floorY) {
            body.y = floorY
            body.velY = if (body.velY > 0f) -body.velY * RESTITUTION else body.velY
            val friction = body.velX * FRICTION
            body.velX = friction
            body.angularVelocity += friction * FLOOR_SPIN_TRANSFER
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
    velY += GRAVITY * dt
    x += velX * dt
    y += velY * dt
    angle += angularVelocity * dt
    velX *= LINEAR_DAMPING
    velY *= LINEAR_DAMPING

    val damped = (angularVelocity * ANGULAR_DAMPING)
        .coerceIn(-MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY)
    // 0 에 가까운 회전은 0 으로 끊는다. 안 그러면 쌓인 종이끼리 매 프레임 미세한 토크를 주고받아
    // 제자리에서 계속 돈다.
    angularVelocity = if (abs(damped) < ANGULAR_SLEEP_THRESHOLD) 0f else damped
}

/** 겹친 두 장을 떼어 놓고, 부딪힌 세기만큼 되튕기며 스친 만큼 돌려 준다. */
private fun resolvePair(a: PaperBody, b: PaperBody) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val distance = sqrt(dx * dx + dy * dy)
    val minDistance = a.radius + b.radius
    // 아직 안 닿았거나, 정확히 겹쳐 어느 쪽으로 밀어낼지 정할 수 없는 경우다.
    if (distance >= minDistance || distance <= MIN_SEPARATION_DISTANCE) return

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

/** 겹친 만큼 서로 반씩 밀어낸다. [POSITION_SLOP] 만큼은 남겨 둬야 다음 프레임에 다시 밀리며 떨지 않는다. */
private fun pushApart(a: PaperBody, b: PaperBody, normalX: Float, normalY: Float, gap: Float) {
    val halfOverlap = (gap - POSITION_SLOP).coerceAtLeast(0f) * 0.5f
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
    val magnitude = -(1f + RESTITUTION) * approachSpeed / 2f
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
    if (abs(tangentSpeed) <= RESTING_TANGENT_SPEED) return

    val spinImpulse = tangentSpeed * PAIR_SPIN_TRANSFER
    a.angularVelocity -= spinImpulse
    b.angularVelocity += spinImpulse
}

// 아래 상수는 대부분 실기기(갤럭시 S23)로 직접 눈으로 보면서 맞춘 경험적 튜닝값이다.
// 단위는 px/s, px/s², rad/s 등 시뮬레이션 내부 단위 기준이며, 다른 값으로 바꾸면 반드시
// 실기기에서 낙하 애니메이션을 재확인해야 한다.
private const val GRAVITY = 2600f
private const val LINEAR_DAMPING = 0.995f
private const val ANGULAR_DAMPING = 0.9f
private const val RESTITUTION = 0.32f
private const val FRICTION = 0.9f
private const val COLLISION_ITERATIONS = 4

// 회전 전달은 일부러 아주 작게 둔다. 종이는 처음 기울기를 대체로 유지하고 부딪힐 때만 살짝 흔들려야 한다.
private const val PAIR_SPIN_TRANSFER = 0.00012f
private const val WALL_SPIN_TRANSFER = 0.0001f
private const val FLOOR_SPIN_TRANSFER = 0.0002f
private const val MAX_ANGULAR_VELOCITY = 0.8f
private const val MIN_SEPARATION_DISTANCE = 1e-4f
private const val ANGULAR_ACTIVITY_WEIGHT = 40f
private const val ANGULAR_SLEEP_THRESHOLD = 0.05f
private const val RESTING_TANGENT_SPEED = 8f
private const val POSITION_SLOP = 0.5f
